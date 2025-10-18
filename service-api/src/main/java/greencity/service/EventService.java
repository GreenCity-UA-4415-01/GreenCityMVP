package greencity.service;

import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import greencity.dto.event.EventPreviewDto;
import greencity.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface EventService {
    /**
     * Method that creates event.
     *
     * @return created event {@link EventDto}
     */
    EventDto createEvent(AddEventDtoRequest request, MultipartFile[] images, Long organizerId);

    /**
     * Method that gets user's joined events with paging and sorting.
     *
     * @param userId        user ID
     * @param eventType     type of events to filter (ONLINE, PLACE, BOTH)
     * @param userLatitude  user's latitude for distance sorting
     * @param userLongitude user's longitude for distance sorting
     * @param pageable      paging parameters
     * @return page of joined events {@link EventPreviewDto}
     */
    Page<EventPreviewDto> getMyEvents(Long userId, EventType eventType, Double userLatitude,
                                      Double userLongitude, Pageable pageable);
}
