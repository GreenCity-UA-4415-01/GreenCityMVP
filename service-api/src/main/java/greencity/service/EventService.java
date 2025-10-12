package greencity.service;

import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import org.springframework.web.multipart.MultipartFile;

public interface EventService {
    /**
     * Method that creates event.
     *
     * @return created event {@link EventDto}
     */
    EventDto createEvent(AddEventDtoRequest request, MultipartFile[] images, Long organizerId);
}
