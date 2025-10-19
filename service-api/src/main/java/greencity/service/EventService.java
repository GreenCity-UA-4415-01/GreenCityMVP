package greencity.service;

import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import org.springframework.web.multipart.MultipartFile;

public interface EventService {
    /**
     * Method that creates event.
     *
     * @param request event creation request
     * @param images event images
     * @param organizerId ID of the user creating the event
     * @return created event {@link EventDto} with computed status
     */
    EventDto createEvent(AddEventDtoRequest request, MultipartFile[] images, Long organizerId);

    /**
     * Get event by ID with computed status (LIVE/UPCOMING/PASSED).
     * Status is computed based on event date/time occurrences:
     * - LIVE: now is between start and finish of at least one occurrence
     * - UPCOMING: all occurrences are in the future
     * - PASSED: all occurrences are in the past
     *
     * @param eventId event ID
     * @return event DTO with computed status, nearestStart, and nearestFinish
     * @note "now" is evaluated as server time in UTC for consistent comparison with database timestamps
     */
    EventDto getEventById(Long eventId);
}
