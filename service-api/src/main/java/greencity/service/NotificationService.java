package greencity.service;

import greencity.dto.PageableAdvancedDto;
import greencity.dto.notification.NotificationDto;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    /**
     * Retrieves all notifications for the authenticated user, ordered by creation date
     * (newest first), with pagination support.
     *
     * @param userId  the ID of the user requesting notifications
     * @param pageable pagination parameters (page, size, sort)
     * @return page of notifications ordered by newest first
     */
    PageableAdvancedDto<NotificationDto> findAllByUserId(Long userId, Pageable pageable);

    /**
     * Marks a single notification as read for the authenticated user.
     * Only the notification owner can mark it as read.
     *
     * @param notificationId the ID of the notification to mark as read
     * @param userId         the ID of the user requesting the operation
     * @return the updated notification DTO
     * @throws greencity.exception.exceptions.NotFoundException if the notification is not found or doesn't belong to the user
     */
    NotificationDto markAsRead(Long notificationId, Long userId);

    /**
     * Marks all unread notifications as read for the authenticated user.
     *
     * @param userId the ID of the user requesting the operation
     * @return the number of notifications marked as read
     */
    int markAllAsRead(Long userId);
}