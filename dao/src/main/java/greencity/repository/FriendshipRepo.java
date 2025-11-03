package greencity.repository;

import greencity.entity.Friendship;
import greencity.entity.FriendshipKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface FriendshipRepo extends JpaRepository<Friendship, FriendshipKey> {
    /**
     * Method to determine whether the {@link Friendship} exists.
     *
     * @param userId   Current User ID.
     * @param friendId Target User ID.
     * @return Boolean result.
     * @author Misha Moroz
     */
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * Method to delete the {@link Friendship} between users.
     *
     * @param userId   Current User ID.
     * @param friendId Target User ID.
     * @author Oleskandr Braiko
     */
    void deleteByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * Count friends of current user.
     *
     * @param userId Current user Id.
     * @return number of friends.
     * @author Misha Moroz
     */
    long countByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = """
    DELETE FROM friendships
    WHERE (user_id = :a AND friend_id = :b)
       OR (user_id = :b AND friend_id = :a)
    """, nativeQuery = true)
    int deleteBothDirections(@Param("a") Long a, @Param("b") Long b);
}