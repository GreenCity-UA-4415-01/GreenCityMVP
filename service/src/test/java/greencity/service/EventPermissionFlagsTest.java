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
 * Unit tests for event permission flags: canEdit, canCancelJoin, isOrganizer.
 * Tests flag computation based on user role, ownership, and event status.
 */
@ExtendWith(SpringExtension.class)
class EventPermissionFlagsTest {
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

    private Event buildEvent(Long id, Long organizerId, OffsetDateTime start, OffsetDateTime finish) {
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
                .startDate(start)
                .finishDate(finish)
                .latitude(50.0)
                .longitude(30.0)
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
    void organizerOfUpcomingEvent_HasCanEditTrue_AndIsOrganizerTrue() {
        Long organizerId = 100L;
        Event event = buildEvent(1L, organizerId,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(organizerId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(organizerId)).thenReturn(UserVO.builder().id(organizerId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(organizerId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertTrue(dto.isCanEdit(), "Organizer should have canEdit=true for upcoming events");
        assertTrue(dto.isOrganizer(), "Organizer should have isOrganizer=true");
        assertTrue(dto.isCanCancelJoin(), "User should be able to cancel join for upcoming events");
    }

    @Test
    void organizerOfPassedEvent_HasCanEditFalse() {
        Long organizerId = 200L;
        Event event = buildEvent(2L, organizerId,
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().minusDays(1));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(organizerId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(organizerId)).thenReturn(UserVO.builder().id(organizerId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(organizerId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertFalse(dto.isCanEdit(), "CanEdit should be false for passed events, even for organizer");
        assertTrue(dto.isOrganizer(), "Organizer flag should still be true");
        assertFalse(dto.isCanCancelJoin(), "Cannot cancel join for passed events");
    }

    @Test
    void admin_HasCanEditTrueForUpcomingEvents() {
        Long adminId = 300L;
        Long eventId = 3L;
        Event event = buildEvent(eventId, adminId,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(adminId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(adminId)).thenReturn(UserVO.builder().id(adminId).role(Role.ROLE_ADMIN).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(adminId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertTrue(dto.isCanEdit(), "Admin should have canEdit=true for upcoming events");
        assertEquals(EventStatus.UPCOMING, dto.getStatus());
    }

    @Test
    void nonOrganizer_HasCanEditFalse_AndIsOrganizerFalse() {
        Long attendeeId = 400L;
        Long organizerId = 999L;
        Event event = buildEvent(4L, organizerId,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventAttenderRepo.findJoinedEventsDefaultSorting(eq(attendeeId), any(), any(Pageable.class))).thenReturn(page);
        when(userService.findById(attendeeId)).thenReturn(UserVO.builder().id(attendeeId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyEvents(attendeeId, null, null, null, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertFalse(dto.isCanEdit(), "Non-organizer should have canEdit=false");
        assertFalse(dto.isOrganizer(), "Non-organizer should have isOrganizer=false");
        assertTrue(dto.isCanCancelJoin(), "Attendee should be able to cancel join for upcoming events");
    }

    @Test
    void liveEvent_DisallowsCancelJoin() {
        Long organizerId = 500L;
        Event event = buildEvent(5L, organizerId,
                OffsetDateTime.now().minusHours(1),
                OffsetDateTime.now().plusHours(1));
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(organizerId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(organizerId)).thenReturn(UserVO.builder().id(organizerId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(organizerId, null, PageRequest.of(0, 10));

        EventPreviewDto dto = result.getContent().get(0);
        assertEquals(EventStatus.LIVE, dto.getStatus());
        assertFalse(dto.isCanCancelJoin(), "Cannot cancel join for live events");
        assertTrue(dto.isCanEdit(), "Organizer can still edit live events");
    }
}

