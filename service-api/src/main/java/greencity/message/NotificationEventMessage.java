package greencity.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Message class for notification events sent via RabbitMQ.
 * This message represents a notification event that will be consumed and persisted.
 *
 * The message includes:
 * - Recipient user IDs (can be multiple for broadcast notifications)
 * - Actor user IDs and usernames (who performed the action)
 * - Action type (e.g., "like.created", "comment.added")
 * - Object information (type, ID, title)
 * - Timestamp when the action occurred
 * - Idempotency key for deduplication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEventMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "Recipient user IDs cannot be empty")
    private List<Long> recipientUserIds;

    @NotEmpty(message = "Actor user IDs cannot be empty")
    private List<Long> actorUserIds;

    private List<String> actorUsernames;

    @NotBlank(message = "Action type cannot be blank")
    @Size(max = 50, message = "Action type must not exceed 50 characters")
    private String actionType;

    @NotBlank(message = "Object type cannot be blank")
    @Size(max = 50, message = "Object type must not exceed 50 characters")
    private String objectType;

    @Size(max = 64, message = "Object ID must not exceed 64 characters")
    private String objectId;

    @Size(max = 255, message = "Object title must not exceed 255 characters")
    private String objectTitle;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime occurredAt;

    @NotBlank(message = "Idempotency key cannot be blank")
    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    private String idempotencyKey;

    private String metadata;
}

