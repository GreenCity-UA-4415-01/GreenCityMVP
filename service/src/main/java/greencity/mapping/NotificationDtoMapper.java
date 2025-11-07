package greencity.mapping;

import greencity.dto.notification.NotificationDto;
import greencity.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationDtoMapper {
    /**
     * Converts {@link Notification} entity to {@link NotificationDto}.
     *
     * @param notification the notification entity to convert
     * @return the converted NotificationDto
     */
    public NotificationDto convert(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .actorUsernames(notification.getActorUsernames())
                .action(notification.getActionType())
                .objectTitle(notification.getObjectTitle())
                .occurredAt(notification.getOccurredAt())
                .isRead(notification.getIsRead())
                .build();
    }
}

