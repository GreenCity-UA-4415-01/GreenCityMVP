package greencity.repository;

import greencity.entity.Friendship;
import greencity.entity.FriendshipKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}