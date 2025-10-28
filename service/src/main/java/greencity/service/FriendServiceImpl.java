package greencity.service;

import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import greencity.entity.User;
import greencity.repository.FriendshipRequestRepo;
import greencity.repository.UserRepo;
import greencity.repository.UserSearchRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private final UserSearchRepo userSearchRepo;
    private final UserRepo userRepo;
    private final FriendshipRequestRepo friendshipRequestRepo;

    @Override
    @Transactional(readOnly = true)
    public PageableDto<UserFriendCardDto> search(Long me, String query, Pageable pageable) {
        Page<User> page = userRepo.searchCandidates(me, query, pageable);

        var cards = page.map(u -> UserFriendCardDto.builder()
                .id(u.getId())
                .name(u.getName())
                .city(u.getCity())
                .profilePicture(u.getProfilePicturePath())
                .personalRate(u.getRating())
                .mutualFriends(0L)     // TODO: add calculation later
                .requestSent(false)    // TODO: we can insert friendship_requests
                .build()
        ).getContent();

        return new PageableDto<>(
                cards,
                page.getTotalElements(),
                page.getNumber(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public void sendFriendRequest(Long me, Long friendId) {
        if (me.equals(friendId)) {
            throw new IllegalArgumentException("You cannot add yourself to friends.");
        }
        // check both users exist
        userRepo.findById(me).orElseThrow(() -> new IllegalArgumentException("User 'me' not found: " + me));
        userRepo.findById(friendId).orElseThrow(() -> new IllegalArgumentException("Friend not found: " + friendId));

        // friends already?
        if (friendshipRequestRepo.areAlreadyFriends(me, friendId)) {
            return; // throw exception
        }
        // have pending status already?
        if (!friendshipRequestRepo.existsPending(me, friendId)) {
            friendshipRequestRepo.insertPending(me, friendId);
        }
    }

    @Override
    @Transactional
    public void cancelFriendRequest(Long me, Long friendId) {
        friendshipRequestRepo.deletePending(me, friendId);
    }
}
