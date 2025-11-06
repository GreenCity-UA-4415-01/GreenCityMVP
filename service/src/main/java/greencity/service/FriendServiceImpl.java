package greencity.service;

import greencity.config.RabbitMQConfig;
import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.user.*;
import greencity.entity.FriendRequest;
import greencity.entity.Friendship;
import greencity.entity.User;
import greencity.exception.exceptions.FriendExistsException;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.SelfFriendException;
import greencity.repository.FriendshipRepo;
import greencity.repository.FriendshipRequestRepo;
import greencity.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private final UserRepo userRepo;
    private final FriendshipRequestRepo friendshipRequestRepo;
    private final FriendshipRepo friendshipRepo;
    private final UserService userService;
    private final AmqpTemplate rabbitTemplate;

    /**
     * Search for friends candidates.
     *
     * @param me       current user id
     * @param query    search string (name/username), may be empty
     * @param pageable pagination settings
     * @return pageable dto of potential friends
     * @author Misha Moroz
     */
    @Override
    @Transactional(readOnly = true)
    public PageableDto<UserFriendCandidateCardDto> search(Long me, String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        if (q.length() > 30) {
            q = q.substring(0, 30);
        }

        Page<User> page = userRepo.searchCandidates(me, q, pageable);

        var cards = page.map(u -> UserFriendCandidateCardDto.builder()
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

    /**
     * Send friend request.
     *
     * @param me       current user id (requester)
     * @param friendId target user id (receiver)
     * @author Misha Moroz
     */
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

        User requester = userRepo.findById(me)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + me));

        Map<String, Object> notificationPayload = new HashMap<>();
        notificationPayload.put("receiverId", friendId);
        notificationPayload.put("content", requester.getName() + " sent you a friend request!");

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.FRIEND_REQUEST_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY,
            notificationPayload);
    }

    /**
     * Cancel friend request.
     *
     * @param me       current user id (requester)
     * @param friendId target user id (receiver)
     * @author Misha Moroz
     */
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
        boolean exists = friendshipRepo.existsByUserIdAndFriendId(userId, friendId)
            || friendshipRepo.existsByUserIdAndFriendId(friendId, userId);
        if (!exists) {
            throw new NotFoundException(ErrorMessage.FRIENDSHIP_NOT_FOUND);
        }
        friendshipRepo.deleteBothDirections(userId, friendId);
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
     *
     * @param me          current user id.
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

        boolean alreadyFriends = (friendshipRepo.existsByUserIdAndFriendId(me, requesterId)
            || friendshipRepo.existsByUserIdAndFriendId(requesterId, me));
        if (alreadyFriends) {
            friendshipRequestRepo.deletePendingOneDirection(requesterId, me);
            return;
        }

        friendshipRepo.save(new Friendship(me, requesterId));
        friendshipRepo.save(new Friendship(requesterId, me));

        friendshipRequestRepo.deletePendingOneDirection(requesterId, me);
        friendshipRequestRepo.deletePendingOneDirection(me, requesterId);
    }

    /**
     * Method to reject friend request.
     *
     * @param me          current user id.
     * @param requesterId id of a user requesting for friendship.
     * @author Misha Moroz
     */
    @Override
    @Transactional
    public void rejectFriendRequest(Long me, Long requesterId) {
        friendshipRequestRepo.deletePending(requesterId, me);
    }

    /**
     * Retrieves a paginated list of the current user's friends, including summary
     * information and the count of mutual friends for each.
     *
     * @param me       The ID of the currently authenticated user.
     * @param pageable {@link Pageable} object for pagination (page, size, sort).
     * @return {@link PageableDto} of {@link UserFriendCardDto} containing paginated
     *         friend data.
     * @author Misha Moroz
     */
    @Override
    @Transactional(readOnly = true)
    public PageableDto<UserFriendCardDto> listFriends(Long me, Pageable pageable) {
        Page<User> page = userRepo.findFriendsPage(me, pageable);
        var cards = page.map(u -> UserFriendCardDto.builder()
            .id(u.getId())
            .name(u.getName())
            .profilePicture(u.getProfilePicturePath())
            .city(u.getCity())
            .personalRate(u.getRating())
            .mutualFriends(userRepo.countMutualFriends(me, u.getId()))
            .build()).getContent();

        return new PageableDto<>(cards, page.getTotalElements(), page.getNumber(), page.getTotalPages());
    }

    /**
     * Retrieves a list of the user's top friends. The logic for determining "top"
     * is handled by the underlying repository query.
     *
     * @param me The ID of the currently authenticated user.
     * @return A {@link List} of {@link FriendShortDto} containing basic friend
     *         information, including online status.
     * @author Misha Moroz
     */
    @Override
    @Transactional(readOnly = true)
    public List<FriendShortDto> topFriends(Long me) {
        var list = userRepo.findTopFriendsForBlock(me);
        return list.stream().map(u -> FriendShortDto.builder()
            .id(u.getId())
            .name(u.getName())
            .profilePicture(u.getProfilePicturePath())
            .online(userService.checkIfTheUserIsOnline(u.getId()))
            .build()).toList();
    }

    /**
     * Retrieves the detailed profile information for a specified friend.
     *
     * @param me       The ID of the currently authenticated user.
     * @param friendId The ID of the user whose profile is being requested.
     * @return {@link FriendProfileDto} containing the friend's full profile
     *         details.
     * @throws NotFoundException if the users are not friends or if the user with
     *                           {@code friendId} does not exist.
     * @author Misha Moroz
     */
    @Override
    @Transactional(readOnly = true)
    public FriendProfileDto friendProfile(Long me, Long friendId) {
        boolean isFriend = friendshipRepo.existsByUserIdAndFriendId(me, friendId)
            || friendshipRepo.existsByUserIdAndFriendId(friendId, me);
        if (!isFriend) {
            throw new NotFoundException(ErrorMessage.FRIENDSHIP_NOT_FOUND);
        }

        var u = userRepo.findById(friendId)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + friendId));

        // int inProgress = 0;
        // int acquired = 0;
        // int tipsCount = 0;
        // int newsCount = 0;

        return FriendProfileDto.builder()
            .id(u.getId())
            .name(u.getName())
            .profilePicture(u.getProfilePicturePath())
            .online(userService.checkIfTheUserIsOnline(u.getId()))
            .personalRate(u.getRating())
            .credo(u.getUserCredo())
            // .habitsInProgress(inProgress)
            // .habitsAcquired(acquired)
            // .tipsAndTricksCount(tipsCount)
            // .newsCount(newsCount)
            .city(u.getCity())
            .build();
    }

    /**
     * Retrieves a paginated list of pending friend requests sent TO the current
     * user. The current user is the receiver of these requests. The results are
     * mapped to include the profile details of the requester.
     *
     * @param userId   The ID of the user who is receiving the requests.
     * @param pageable Pagination settings.
     * @return {@link PageableDto} of {@link UserFriendCandidateCardDto} containing
     *         paginated requester data.
     * @author Oleksandr Braiko
     */
    @Override
    @Transactional(readOnly = true)
    public PageableDto<UserFriendCandidateCardDto> friendRequests(Long userId, Pageable pageable) {
        Page<FriendRequest> requestPage = friendshipRequestRepo.findAllPendingRequestsByReceiverId(userId, pageable);

        List<Long> requesterIds = requestPage.getContent().stream()
            .map(FriendRequest::getRequesterId)
            .toList();

        if (requesterIds.isEmpty()) {
            return new PageableDto<>(List.of(), 0L, requestPage.getNumber(), 0);
        }

        List<User> requesters = userRepo.findAllById(requesterIds);
        Map<Long, User> requesterMap = requesters.stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserFriendCandidateCardDto> dtos = requesterIds.stream()
            .filter(requesterMap::containsKey)
            .map(requesterId -> {
                User u = requesterMap.get(requesterId);

                return UserFriendCandidateCardDto.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .profilePicture(u.getProfilePicturePath())
                    .city(u.getCity())
                    .personalRate(u.getRating())
                    .requestSent(false)
                    .mutualFriends(0L)
                    .build();
            })
            .toList();

        return new PageableDto<>(
            dtos,
            requestPage.getTotalElements(),
            requestPage.getNumber(),
            requestPage.getTotalPages());
    }
}
