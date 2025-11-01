package greencity.dto.event;

import java.util.List;

public interface EventRequest {
    String getTitle();

    String getDescription();

    List<EventDateLocationDto> getDatesLocations();
}
