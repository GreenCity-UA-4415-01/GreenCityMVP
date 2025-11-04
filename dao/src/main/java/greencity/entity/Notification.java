package greencity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_created_at", columnList = "recipient_user_id, created_at DESC"),
                @Index(name = "idx_notifications_recipient_is_read_created_at", columnList = "recipient_user_id, is_read, created_at DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_idempotency_key", columnNames = {"idempotency_key"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notifications_recipient_user"))
    private User recipient;

    @Column(name = "actor_usernames", nullable = false, length = 255)
    private String actorUsernames;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "object_type", nullable = false, length = 50)
    private String objectType;

    @Column(name = "object_id", length = 64)
    private String objectId;

    @Column(name = "object_title", length = 255)
    private String objectTitle;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;
}


