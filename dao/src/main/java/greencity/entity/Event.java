package greencity.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 70, nullable = false)
    private String title;

    @Column(length = 63206, nullable = false)
    private String description;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(name = "title_image")
    private String titleImage;

    @Column(name = "created_at", nullable = false, updatable = false)
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

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventDateTimeLocation> dateTimeLocations;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventImage> images;

    /*
     * @ManyToMany
     *
     * @JoinTable( name = "events_tags", joinColumns = @JoinColumn(name =
     * "event_id"), inverseJoinColumns = @JoinColumn(name = "tag_id") ) private
     * List<Tag> tags;
     */
}
