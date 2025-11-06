package greencity.dto.notification;

import lombok.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Data
@Builder
public class NotificationDto {
    private Long id;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;
}
