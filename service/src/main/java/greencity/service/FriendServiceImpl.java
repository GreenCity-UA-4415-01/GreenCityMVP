package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import greencity.entity.Friendship;
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
        validateUsersPair(me, friendId);

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

    /**
     * Method to delete {@link Friendship} between Users.
     *
     * @param userId   Current User Id.
     * @param friendId Target User Id.
     * @author Oleksandr Braiko
     */
    @Override
    @Transactional
    public void unfriendUser(Long userId, Long friendId) {
        validateUsersPair(userId, friendId);
        if (!friendshipRepo.existsByUserIdAndFriendId(userId, friendId)) {
            throw new NotFoundException(ErrorMessage.FRIENDSHIP_NOT_FOUND);
        }
        friendshipRepo.deleteByUserIdAndFriendId(userId, friendId);
    }

    /**
     * Helper method to ensure two users are not equal and both exist.
     *
     * @param userId   Current User Id.
     * @param friendId Target User Id.
     * @author Oleksandr Braiko
     */
    private void validateUsersPair(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new SelfFriendException(ErrorMessage.USER_CANT_SELF_FRIEND);
        }
        userRepo.findById(userId).orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + userId));
        userRepo.findById(friendId)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + friendId));
    }

    /**
     * Method to accept friend request.
     * @param me current user id.
     * @param requesterId id of a user requesting for friendship.
     * @author Misha Moroz
     */
    @Override
    @Transactional
    public void acceptFriendRequest(Long me, Long requesterId) {
        if (me.equals(requesterId)) {
            throw new SelfFriendException(ErrorMessage.USER_CANT_SELF_FRIEND);
        }
        if (!friendshipRequestRepo.existsPending(requesterId, me)) {
            throw new NotFoundException(ErrorMessage.FRIENDSHIP_REQUEST_NOT_FOUND);
        }
        if (friendshipRequestRepo.areAlreadyFriends(me, requesterId)) {
            friendshipRequestRepo.deletePending(requesterId, me);
            return;
        }
        friendshipRepo.save(new Friendship(me, requesterId));
        friendshipRepo.save(new Friendship(requesterId, me));

        friendshipRequestRepo.deletePendingOneDirection(requesterId, me);
    }

    /**
     * Method to reject friend request.
     * @param me current user id.
     * @param requesterId id of a user requesting for friendship.
     * @author Misha Moroz
     */
    @Override
    @Transactional
    public void rejectFriendRequest(Long me, Long requesterId) {
        friendshipRequestRepo.deletePending(requesterId, me);
    }
}
