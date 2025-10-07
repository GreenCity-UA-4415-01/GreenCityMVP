package greencity.entity;

import greencity.enums.NewsletterSubscriptionSource;
import greencity.enums.NewsletterSubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Table(name = "newsletter_subscriptions")
public class NewsletterSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NewsletterSubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private NewsletterSubscriptionSource source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedDate;
}
