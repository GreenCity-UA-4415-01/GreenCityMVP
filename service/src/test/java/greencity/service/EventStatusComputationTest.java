package greencity.service;

import greencity.dto.event.EventPreviewDto;
import greencity.entity.Event;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import greencity.enums.EventStatus;
import greencity.enums.Role;
import greencity.dto.user.UserVO;
import greencity.repository.EventAttenderRepo;
import greencity.repository.EventDateTimeLocationRepo;
import greencity.repository.EventImageRepo;
import greencity.repository.EventRepo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for event status computation logic.
 * Tests UPCOMING, LIVE, and PASSED status determination based on event date/time occurrences.
 */
@ExtendWith(SpringExtension.class)
class EventStatusComputationTest {
    @Mock
    private EventRepo eventRepository;
    @Mock
    private EventDateTimeLocationRepo dateTimeLocationRepository;
    @Mock
    private EventImageRepo eventImageRepository;
    @Mock
    private EventAttenderRepo eventAttenderRepo;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private UserService userService;
    @Mock
    private ModelMapper mapper;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event buildEvent(Long id, Long organizerId, List<EventDateTimeLocation> locations) {
        Event event = Event.builder()
                .id(id)
                .title("Event " + id)
                .description("Test")
                .open(true)
                .organizerId(organizerId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        event.setDateTimeLocations(locations);
        EventImage img = EventImage.builder()
                .event(event)
                .imagePath("img.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now())
                .build();
        event.setImages(List.of(img));
        return event;
    }

    private EventDateTimeLocation buildLocation(OffsetDateTime start, OffsetDateTime finish) {
        return EventDateTimeLocation.builder()
                .startDate(start)
                .finishDate(finish)
                .latitude(50.0)
                .longitude(30.0)
                .build();
    }

    @Test
    void eventWithFutureStart_ReturnsUpcoming() {
        Long userId = 100L;
        OffsetDateTime futureStart = OffsetDateTime.now().plusDays(1);
        Event event = buildEvent(1L, userId, List.of(buildLocation(
                futureStart,
                futureStart.plusHours(2)
        )));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        assertEquals(EventStatus.UPCOMING, result.getContent().get(0).getStatus());
    }

    @Test
    void eventWithPastStartPresentFinish_ReturnsLive() {
        Long userId = 200L;
        OffsetDateTime pastStart = OffsetDateTime.now().minusHours(1);
        OffsetDateTime futureFinish = OffsetDateTime.now().plusHours(1);
        Event event = buildEvent(2L, userId, List.of(buildLocation(pastStart, futureFinish)));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        assertEquals(EventStatus.LIVE, result.getContent().get(0).getStatus());
    }

    @Test
    void eventWithPastFinish_ReturnsPassed() {
        Long userId = 300L;
        OffsetDateTime pastStart = OffsetDateTime.now().minusDays(2);
        OffsetDateTime pastFinish = OffsetDateTime.now().minusDays(1);
        Event event = buildEvent(3L, userId, List.of(buildLocation(pastStart, pastFinish)));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        assertEquals(EventStatus.PASSED, result.getContent().get(0).getStatus());
    }

    @Test
    void eventWithMultipleOccurrences_ReturnsCorrectNearestDates() {
        Long userId = 400L;
        OffsetDateTime future1 = OffsetDateTime.now().plusDays(1);
        OffsetDateTime future2 = OffsetDateTime.now().plusDays(3);
        OffsetDateTime future3 = OffsetDateTime.now().plusDays(5);

        Event event = buildEvent(4L, userId, List.of(
                buildLocation(future3, future3.plusHours(2)),
                buildLocation(future1, future1.plusHours(2)),
                buildLocation(future2, future2.plusHours(2))
        ));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertEquals(future1, dto.getNearestStart());
        assertEquals(future1.plusHours(2), dto.getNearestFinish());
        assertEquals(EventStatus.UPCOMING, dto.getStatus());
    }

    @Test
    void eventWithPassedAndUpcomingOccurrences_ReturnsUpcomingStatus() {
        Long userId = 500L;
        OffsetDateTime past = OffsetDateTime.now().minusDays(1);
        OffsetDateTime future = OffsetDateTime.now().plusDays(1);

        Event event = buildEvent(5L, userId, List.of(
                buildLocation(past, past.plusHours(2)),
                buildLocation(future, future.plusHours(2))
        ));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertEquals(EventStatus.UPCOMING, dto.getStatus());
        assertEquals(future, dto.getNearestStart());
    }
}

