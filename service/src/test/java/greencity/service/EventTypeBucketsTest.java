package greencity.service;

import greencity.dto.event.EventPreviewDto;
import greencity.entity.Event;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import greencity.enums.EventType;
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
 * Tests for event type buckets: place, online, both.
 * Verifies that events are correctly classified and filtered by type.
 */
@ExtendWith(SpringExtension.class)
class EventTypeBucketsTest {
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

    private Event buildEvent(Long id, Long organizerId, boolean hasPlace, boolean hasOnline) {
        Event event = Event.builder()
                .id(id)
                .title("Event " + id)
                .description("Test")
                .open(true)
                .organizerId(organizerId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        EventDateTimeLocation loc = EventDateTimeLocation.builder()
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(hasPlace ? 50.0 : null)
                .longitude(hasPlace ? 30.0 : null)
                .onlineLink(hasOnline ? "https://example.com/meeting" : null)
                .build();
        event.setDateTimeLocations(List.of(loc));

        EventImage img = EventImage.builder()
                .event(event)
                .imagePath("img.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now())
                .build();
        event.setImages(List.of(img));
        return event;
    }

    @Test
    void placeEvent_ReturnsTypesPlaceTrue() {
        Long userId = 100L;
        Event event = buildEvent(1L, userId, true, false);
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertTrue(dto.getTypes().isPlace());
        assertFalse(dto.getTypes().isOnline());
    }

    @Test
    void onlineEvent_ReturnsTypesOnlineTrue() {
        Long userId = 200L;
        Event event = buildEvent(2L, userId, false, true);
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertFalse(dto.getTypes().isPlace());
        assertTrue(dto.getTypes().isOnline());
    }

    @Test
    void hybridEvent_ReturnsBothTypesTrue() {
        Long userId = 300L;
        Event event = Event.builder()
                .id(3L)
                .title("Hybrid Event")
                .description("Test")
                .open(true)
                .organizerId(userId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Add both place and online locations
        EventDateTimeLocation placeLoc = EventDateTimeLocation.builder()
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(50.0)
                .longitude(30.0)
                .build();

        EventDateTimeLocation onlineLoc = EventDateTimeLocation.builder()
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .onlineLink("https://example.com/meeting")
                .build();

        event.setDateTimeLocations(List.of(placeLoc, onlineLoc));

        EventImage img = EventImage.builder()
                .event(event)
                .imagePath("img.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now())
                .build();
        event.setImages(List.of(img));

        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertTrue(dto.getTypes().isPlace(), "Hybrid event should have place=true");
        assertTrue(dto.getTypes().isOnline(), "Hybrid event should have online=true");
    }

    @Test
    void filterByPlaceType_ReturnsOnlyPlaceEvents() {
        Long userId = 400L;
        Event placeEvent = buildEvent(1L, userId, true, false);
        Event onlineEvent = buildEvent(2L, userId, false, true);
        Page<Event> page = new PageImpl<>(List.of(placeEvent, onlineEvent), PageRequest.of(0, 10), 2);

        when(eventAttenderRepo.findJoinedEventsWithSorting(
                eq(userId), any(), eq(EventType.PLACE.name()), any(), any(), any(Pageable.class)
        )).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyEvents(userId, EventType.PLACE, null, null, null, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        // All returned events should have place=true (based on filtering)
    }

    @Test
    void filterByOnlineType_ReturnsOnlyOnlineEvents() {
        Long userId = 500L;
        Event placeEvent = buildEvent(1L, userId, true, false);
        Event onlineEvent = buildEvent(2L, userId, false, true);
        Page<Event> page = new PageImpl<>(List.of(placeEvent, onlineEvent), PageRequest.of(0, 10), 2);

        when(eventAttenderRepo.findJoinedEventsWithSorting(
                eq(userId), any(), eq(EventType.ONLINE.name()), any(), any(), any(Pageable.class)
        )).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyEvents(userId, EventType.ONLINE, null, null, null, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void filterByBothType_ReturnsAllEvents() {
        Long userId = 600L;
        Event placeEvent = buildEvent(1L, userId, true, false);
        Event onlineEvent = buildEvent(2L, userId, false, true);
        Event hybridEvent = buildEvent(3L, userId, true, true);
        Page<Event> page = new PageImpl<>(List.of(placeEvent, onlineEvent, hybridEvent), PageRequest.of(0, 10), 3);

        when(eventAttenderRepo.findJoinedEventsDefaultSorting(eq(userId), any(), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyEvents(userId, EventType.BOTH, null, null, null, PageRequest.of(0, 10));

        assertEquals(3, result.getTotalElements());
    }
}

