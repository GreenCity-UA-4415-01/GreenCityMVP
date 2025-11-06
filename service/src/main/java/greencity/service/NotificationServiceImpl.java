package greencity.service;

import greencity.dto.notification.NotificationDto;
import greencity.entity.Notification;
import greencity.repository.NotificationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepo notificationRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepo notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // --- Mapper Function ---

    /**
     * Converts a JPA Notification Entity to a NotificationDTO.
     */
    private NotificationDto toNotificationDTO(Notification notification) {
        return NotificationDto.builder()
            .id(notification.getId())
            .content(notification.getContent())
            .sentAt(notification.getSentAt())
            .isRead(notification.isRead())
            .build();
    }

    // --- Persistence Methods ---

    /**
     * Creates a Notification entity and persists it. This is called by the RabbitMQ
     * listener.
     */
    @Override
    @Transactional
    public NotificationDto saveNotification(Long userId, String content) {
        Notification notification = Notification.builder()
            .userId(userId)
            .content(content)
            .sentAt(LocalDateTime.now())
            .isRead(false)
            .build();

        notificationRepository.save(notification);

        return toNotificationDTO(notification);
    }

    /**
     * Fetches all notifications for a user, converts them to DTOs, and returns the
     * list.
     */
    @Override
    public List<NotificationDto> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId).stream()
            .map(this::toNotificationDTO)
            .toList();
    }

    /**
     * Fetches the count of unread notifications for a user.
     */
    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Marks all a user's unread notifications as read.
     */
    @Override
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }
}
