package greencity.dto.event;

import greencity.enums.EventStatus;
import lombok.*;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
public class EventPreviewDto {
    private Long id;
    private String title;
    private String description;
    private boolean open;
    private Long organizerId;
    private String titleImage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private EventStatus status;
    private OffsetDateTime nearestStart;
    private boolean canCancelJoin;
    private boolean isFavourite;
    private boolean isSubscribed;
    private String visibility;
    private Double latitude;
    private Double longitude;
    private String onlineLink;
}
