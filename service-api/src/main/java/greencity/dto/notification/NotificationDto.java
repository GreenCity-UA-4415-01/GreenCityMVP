package greencity.dto.notification;

import lombok.*;
import java.time.OffsetDateTime;

/**
 * DTO for notification response.
 * Contains notification information to be displayed to the user.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
public class NotificationDto {
    private Long id;
    private String actorUsernames;
    private String action;
    private String objectTitle;
    private OffsetDateTime occurredAt;
    private Boolean isRead;
}

