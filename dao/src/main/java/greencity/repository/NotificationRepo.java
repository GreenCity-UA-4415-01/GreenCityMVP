package greencity.repository;

import greencity.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Spring Data JPA Repository for the Notification entity. Includes standard
 * CRUD (inherited) and custom Read/Update operations for notifications.
 */
@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
    /**
     * Finds all notifications for a specific user, ordered by most recent first.
     * Uses Spring Data JPA's derived query method (Read 1).
     *
     * @param userId The ID of the recipient user.
     * @return A list of Notification entities.
     */
    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);

    /**
     * Counts the number of unread notifications for a specific user (Read 2). Used
     * typically for displaying a notification badge count.
     *
     * @param userId The ID of the user.
     * @return The count of unread notifications.
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Marks all unread notifications for a given user as read in a single batch
     * operation (Update).
     *
     * @param userId The ID of the user whose notifications should be marked as
     *               read.
     * @return The number of rows updated.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(Long userId);

    // Standard CRUD operations (Create, Read by ID, Delete) are inherited from
    // JpaRepository.
}
