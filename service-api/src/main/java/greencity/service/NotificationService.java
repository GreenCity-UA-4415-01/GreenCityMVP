package greencity.service;

import java.util.List;
import greencity.dto.notification.NotificationDto;

/**
 * Service interface for managing Notification entities and business logic.
 * Decouples the NotificationListener and Controllers from direct repository
 * access.
 */
public interface NotificationService {
    /**
     * Creates and persists a new notification based on an incoming event.
     *
     * @param userId  The ID of the user who should receive the notification.
     * @param content The message content of the notification.
     * @return The newly saved Notification entity.
     */
    NotificationDto saveNotification(Long userId, String content);

    /**
     * Retrieves all notifications for a specific user, sorted by most recent first.
     *
     * @param userId The ID of the user.
     * @return A list of Notification entities.
     */
    List<NotificationDto> getNotificationsByUserId(Long userId);

    /**
     * Retrieves the count of unread notifications for a specific user.
     *
     * @param userId The ID of the user.
     * @return The count of unread notifications.
     */
    long getUnreadCount(Long userId);

    /**
     * Marks all unread notifications for a user as read.
     *
     * @param userId The ID of the user.
     * @return The number of notifications updated.
     */
    int markAllAsRead(Long userId);
}
