package greencity.repository;

import greencity.entity.FriendRequest;
import greencity.entity.FriendRequestKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FriendRequestRepo extends JpaRepository<FriendRequest, FriendRequestKey> {
    Optional<FriendRequest> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId, String status);

    void deleteByRequesterIdAndReceiverId(Long requesterId, Long receiverId);
}