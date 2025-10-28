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
public class FriendSearchServiceImpl implements FriendSearchService {
    private final UserSearchRepo userSearchRepo;
    private final UserRepo userRepo;
    private final FriendshipRequestRepo friendshipRequestRepo;

    @Override
    @Transactional(readOnly = true)
    public PageableDto<UserFriendCardDto> search(Long me, String query, Pageable pageable) {
        Page<User> page = userSearchRepo.searchCandidates(me, query, pageable);

        var cards = page.map(u -> UserFriendCardDto.builder()
                .id(u.getId())
                .name(u.getName())
                .city(u.getCity())
                .profilePicture(u.getProfilePicturePath())
                .personalRate(u.getRating())
                .mutualFriends(0L)     // TODO: добавим расчёт позже
                .requestSent(false)    // TODO: можно подставлять из friendship_requests
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
        // проверим, что оба пользователя существуют
        userRepo.findById(me).orElseThrow(() -> new IllegalArgumentException("User 'me' not found: " + me));
        userRepo.findById(friendId).orElseThrow(() -> new IllegalArgumentException("Friend not found: " + friendId));

        // уже друзья?
        if (friendshipRequestRepo.areAlreadyFriends(me, friendId)) {
            return; // или бросаем исключение — на твой вкус
        }
        // уже есть pending?
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
