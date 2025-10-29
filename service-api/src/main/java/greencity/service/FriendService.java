package greencity.service;

import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import org.springframework.data.domain.Pageable;

public interface FriendService {
    PageableDto<UserFriendCardDto> search(Long me, String query, Pageable pageable);

    /**
     * Method to send friend request to target user by the current user.
     *
     * @param me       Current user id.
     * @param friendId target user id.
     */
    void sendFriendRequest(Long me, Long friendId);

    /**
     * Method to cancel friendship request by the requester.
     *
     * @param me       Current user id.
     * @param friendId target user id.
     */
    void cancelFriendRequest(Long me, Long friendId); // опційно (кнопка “cancel request”)

    /**
     * Method to break friendship between users.
     *
     * @param userId   Current user id.
     * @param friendId target user id.
     */
    void unfriendUser(Long userId, Long friendId);

    void acceptFriendRequest(Long me, Long requesterId);
    void rejectFriendRequest(Long me, Long requesterId);

}
