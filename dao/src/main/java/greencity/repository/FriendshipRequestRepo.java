package greencity.repository;

import greencity.entity.FriendRequest;
import greencity.entity.FriendRequestKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FriendshipRequestRepo extends JpaRepository<FriendRequest, FriendRequestKey> {
    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = """
                INSERT INTO friendship_requests (requester_id, receiver_id, status)
                VALUES (:me, :friendId, 'PENDING')
                ON CONFLICT (requester_id, receiver_id) DO NOTHING
            """)
    void insertPending(@Param("me") Long me, @Param("friendId") Long friendId);

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = """
                DELETE FROM friendship_requests
                WHERE requester_id = :me
                            AND receiver_id = :friendId
                            AND status = 'PENDING'
            """)
    int deletePending(@Param("me") Long me, @Param("friendId") Long friendId);

    @Query(
        nativeQuery = true,
        value = """
                SELECT EXISTS(
                    SELECT 1 FROM friendship_requests
                    WHERE requester_id = :me
                                  AND receiver_id = :friendId
                                  AND status = 'PENDING'
                )
            """)
    boolean existsPending(@Param("me") Long me, @Param("friendId") Long friendId);

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = """
                DELETE FROM friendship_requests
                WHERE requester_id = :a AND receiver_id = :b AND status = 'PENDING'
            """)
    int deletePendingOneDirection(@Param("a") Long a, @Param("b") Long b);

    @Query(
        nativeQuery = true,
        value = """
                SELECT EXISTS(
                    SELECT 1 FROM friendships
                    WHERE (user_id = :me AND friend_id = :friendId)
                       OR (user_id = :friendId AND friend_id = :me)
                )
            """)
    boolean areAlreadyFriends(@Param("me") Long me, @Param("friendId") Long friendId);

    /**
     * Retrieves a paginated list of pending friend requests where the current user
     * is the receiver.
     *
     * @param receiverId The ID of the user receiving the requests (the current
     *                   user).
     * @param pageable   Pagination information.
     * @return A page of FriendRequest entities.
     */
    @Query(
        nativeQuery = true,
        value = """
                SELECT * FROM friendship_requests
                WHERE receiver_id = :receiverId
                AND status = 'PENDING'
            """,
        countQuery = """
                SELECT COUNT(*)
                FROM friendship_requests
                WHERE receiver_id = :receiverId
                AND status = 'PENDING'
            """)
    Page<FriendRequest> findAllPendingRequestsByReceiverId(@Param("receiverId") Long receiverId, Pageable pageable);
}