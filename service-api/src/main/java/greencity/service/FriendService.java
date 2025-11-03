package greencity.service;

import greencity.dto.PageableDto;
import greencity.dto.user.FriendProfileDto;
import greencity.dto.user.FriendShortDto;
import greencity.dto.user.UserFriendCandidateCardDto;
import greencity.dto.user.UserFriendCardDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Friend domain operations: search, request lifecycle, friendship management,
 * listings for UI, and friend profile retrieval.
 */
public interface FriendService {
    /**
     * Search users who are NOT yet friends with the current user.
     *
     * @param me     current user id
     * @param query  search string (name/username), may be empty
     * @param pageable pagination settings
     * @return paged list of candidate users formatted for friend cards
     */
    PageableDto<UserFriendCandidateCardDto> search(Long me, String query, Pageable pageable);

    /**
     * Send a friend request (me -> friendId).
     *
     * @param me       current user id (requester)
     * @param friendId target user id (receiver)
     */
    void sendFriendRequest(Long me, Long friendId);

    /**
     * Cancel an outbound friend request previously sent by the current user.
     *
     * @param me       current user id (requester)
     * @param friendId target user id (receiver)
     */
    void cancelFriendRequest(Long me, Long friendId);

    /**
     * Accept an incoming friend request (requesterId -> me) and create friendship.
     *
     * @param me          current user id (receiver)
     * @param requesterId requester user id
     */
    void acceptFriendRequest(Long me, Long requesterId);

    /**
     * Reject an incoming friend request (requesterId -> me).
     *
     * @param me          current user id (receiver)
     * @param requesterId requester user id
     */
    void rejectFriendRequest(Long me, Long requesterId);

    /**
     * Remove friendship in both directions (me <-> friendId).
     *
     * @param me       current user id
     * @param friendId friend user id to unfriend
     */
    void unfriendUser(Long me, Long friendId);

    /**
     * Full paginated list for the "All friends" tab.
     *
     * @param me       current user id
     * @param pageable pagination settings
     * @return paged list of friend cards
     */
    PageableDto<UserFriendCardDto> listFriends(Long me, Pageable pageable);

    /**
     * Up to 6 top friends for the "My Habits" widget (BR sorting rules).
     *
     * @param me current user id
     * @return up to six short friend entries
     */
    List<FriendShortDto> topFriends(Long me);

    /**
     * Detailed friend profile for friend page navigation.
     *
     * @param me       current user id
     * @param friendId friend user id
     * @return friend profile dto
     */
    FriendProfileDto friendProfile(Long me, Long friendId);
}
