package greencity.dto.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
public class EventDto {
    private Long id;
    private String title;
    private String description;
    private boolean open;
    private Long organizerId;
    private String titleImage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<EventDateLocationDto> datesLocations;
    private List<String> imageUrls;
}
