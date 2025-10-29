package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import greencity.entity.Friendship;
import greencity.entity.FriendshipKey;
import greencity.entity.User;
import greencity.exception.exceptions.FriendExistsException;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.SelfFriendException;
import greencity.repository.FriendshipRepo;
import greencity.repository.FriendshipRequestRepo;
import greencity.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private final UserRepo userRepo;
    private final FriendshipRequestRepo friendshipRequestRepo;
    private final FriendshipRepo friendshipRepo;

    @Override
    @Transactional(readOnly = true)
    public PageableDto<UserFriendCardDto> search(Long me, String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        if (q.length() > 30) q = q.substring(0, 30);

        Page<User> page = userRepo.searchCandidates(me, q, pageable);

        var cards = page.map(u -> UserFriendCardDto.builder()
                .id(u.getId())
                .name(u.getName())
                .city(u.getCity())
                .profilePicture(u.getProfilePicturePath())
                .personalRate(u.getRating())
                .mutualFriends(0L)
                .requestSent(friendshipRequestRepo.existsPending(me, u.getId()))
                .build()).getContent();

        return new PageableDto<>(cards, page.getTotalElements(), page.getNumber(), page.getTotalPages());
    }

    @Override
    @Transactional
    public void sendFriendRequest(Long me, Long friendId) {
        if (me.equals(friendId)) {
            throw new SelfFriendException(ErrorMessage.USER_CANT_SELF_FRIEND);
        }
        userRepo.findById(me).orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + me));
        userRepo.findById(friendId).orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + friendId));

        if (friendshipRequestRepo.areAlreadyFriends(me, friendId)) {
            throw new FriendExistsException(ErrorMessage.FRIENDSHIP_ALREADY_EXISTS);
        }
        if (friendshipRequestRepo.existsPending(me, friendId)) {
            throw new FriendExistsException(ErrorMessage.FRIENDSHIP_REQUEST_ALREADY_EXISTS);
        }
        friendshipRequestRepo.insertPending(me, friendId);
    }

    @Override
    @Transactional
    public void cancelFriendRequest(Long me, Long friendId) {
        friendshipRequestRepo.deletePending(me, friendId);
    }

    // ===== НОВЕ =====
    @Override
    @Transactional
    public void acceptFriendRequest(Long me, Long requesterId) {
        if (me.equals(requesterId)) {
            throw new SelfFriendException(ErrorMessage.USER_CANT_SELF_FRIEND);
        }
        // має існувати pending З БОКУ requester -> me
        if (!friendshipRequestRepo.existsPending(requesterId, me)) {
            throw new NotFoundException(ErrorMessage.FRIENDSHIP_REQUEST_NOT_FOUND);
        }
        // якщо вже друзі — нічого не робимо / або кинути 409
        if (friendshipRequestRepo.areAlreadyFriends(me, requesterId)) {
            friendshipRequestRepo.deletePending(requesterId, me); // підчистити, щоб не висіло
            return;
        }

        // створюємо дві спрямовані дружби
        friendshipRepo.save(new Friendship(me, requesterId));
        friendshipRepo.save(new Friendship(requesterId, me));

        // помітити як accepted (або видалити)
        friendshipRequestRepo.markAccepted(requesterId, me);

        // прибрати можливий зустрічний pending (me -> requester), якщо був
        friendshipRequestRepo.deletePendingOneDirection(me, requesterId);
    }

    @Override
    @Transactional
    public void rejectFriendRequest(Long me, Long requesterId) {
        // просто видаляємо pending (requester -> me), якщо існує
        friendshipRequestRepo.deletePending(requesterId, me);
    }
}
