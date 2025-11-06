package greencity.repository;

import greencity.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides an interface to manage {@link Notification} entity.
 * Supports pagination and ordering for efficient notification retrieval.
 */
@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
    /**
     * Finds all notifications for a specific recipient, ordered by creation date
     * (newest first), with pagination support.
     * Uses the index idx_notifications_recipient_created_at for optimal performance.
     *
     * @param recipientId the ID of the notification recipient
     * @param pageable    pagination parameters (page, size, sort)
     * @return page of notifications ordered by newest first
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            @Param("recipientId") Long recipientId,
            Pageable pageable
    );

    /**
     * Finds notifications for a specific recipient filtered by read status,
     * ordered by creation date (newest first), with pagination support.
     * Uses the index idx_notifications_recipient_is_read_created_at for optimal performance.
     *
     * @param recipientId the ID of the notification recipient
     * @param isRead      filter by read status (true for read, false for unread)
     * @param pageable    pagination parameters (page, size, sort)
     * @return page of notifications filtered by read status, ordered by newest first
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = :isRead ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(
            @Param("recipientId") Long recipientId,
            @Param("isRead") Boolean isRead,
            Pageable pageable
    );

    /**
     * Finds a notification by ID and recipient ID.
     * Used to ensure that users can only access their own notifications.
     *
     * @param id          the notification ID
     * @param recipientId the ID of the notification recipient
     * @return the notification if found, null otherwise
     */
    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.recipient.id = :recipientId")
    Notification findByIdAndRecipientId(
            @Param("id") Long id,
            @Param("recipientId") Long recipientId
    );

    /**
     * Marks all unread notifications as read for a specific recipient.
     * Uses a bulk update query for efficiency.
     *
     * @param recipientId the ID of the notification recipient
     * @return the number of notifications updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId);
}

