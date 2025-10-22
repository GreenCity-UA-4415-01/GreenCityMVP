package greencity.service;

import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import greencity.dto.user.UserVO;
import greencity.dto.event.EventPreviewDto;
import greencity.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface EventService {
    /**
     * Method that creates event.
     *
     * @param request     event creation request
     * @param images      event images
     * @param organizerId ID of the user creating the event
     * @return created event {@link EventDto} with computed status
     */
    EventDto createEvent(AddEventDtoRequest request, MultipartFile[] images, Long organizerId);

    /**
     * Method for deleting the {@link EventDto} instance by its id.
     *
     * @param id   - {@link EventDto} instance id which will be deleted.
     * @param user current {@link UserVO} that wants to delete.
     */
    void deleteEvent(Long id, UserVO user);

    /**
     * Method for getting the {@link EventDto} instance by its id.
     *
     * @param id {@link EventDto} instance id.
     * @return {@link EventDto} instance.
     */
    EventDto findById(Long id);

    /**
     * Returns all events visible to the given user. Open events are visible to
     * everyone, closed — only to organizer’s friends.
     *
     * @param userVO current authenticated user
     * @return list of visible events
     */
    List<EventDto> getVisibleEvents(UserVO userVO);

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

    /**
     * Method that gets events created by the current user with capability flags for
     * editing. Returns events with canEdit flag set to true for organizers and
     * admins. Default sorting by nearestStart.
     *
     * @param userId   user ID of the current user
     * @param pageable paging parameters
     * @return page of created events {@link EventPreviewDto} with canEdit flags
     */
    Page<EventPreviewDto> getMyCreatedEvents(Long userId, Pageable pageable);

    /**
     * Get event by ID with computed status (LIVE/UPCOMING/PASSED). Status is
     * computed based on event date/time occurrences: - LIVE: now is between start
     * and finish of at least one occurrence. - UPCOMING: all occurrences are in the
     * future. - PASSED: all occurrences are in the past. "now" is evaluated as
     * server time in UTC for consistent comparison with database timestamps.
     *
     * @param eventId event ID
     * @return event DTO with computed status, nearestStart, and nearestFinish
     */
    EventDto getEventById(Long eventId);
}