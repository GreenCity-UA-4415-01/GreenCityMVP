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
    private String titleImage;
    private EventStatus status;
    private OffsetDateTime nearestStart;
    private OffsetDateTime nearestFinish;
    private EventTypesDto types;
    private Double distance;
    private String visibility; // "open" | "closed"
    private boolean canCancelJoin;
    private boolean canEdit;
    private boolean isFavourite;
    private boolean isSubscribed;
    private boolean isOrganizer;
}
