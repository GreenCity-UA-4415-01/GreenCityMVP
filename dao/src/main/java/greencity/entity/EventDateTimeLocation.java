package greencity.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "event_date_time_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDateTimeLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "finish_date", nullable = false)
    private OffsetDateTime finishDate;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "online_link", length = 500)
    private String onlineLink;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
