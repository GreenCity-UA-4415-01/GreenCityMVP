package greencity.repository;

import greencity.entity.Friendship;
import greencity.entity.FriendshipKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepo extends JpaRepository<Friendship, FriendshipKey> {
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    long countByUserId(Long userId);
}