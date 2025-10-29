package greencity.service;

import greencity.dto.event.EventPreviewDto;
import greencity.entity.Event;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import greencity.enums.EventStatus;
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

@ExtendWith(SpringExtension.class)
class EventPreviewMappingTest {
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

    private Event buildPlaceEvent(Long id, Long organizerId, OffsetDateTime start, OffsetDateTime finish) {
        Event event = Event.builder()
                .id(id)
                .title("Title " + id)
                .description("Desc")
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
        EventImage img = EventImage.builder().event(event).imagePath("main.jpg").main(true).createdAt(OffsetDateTime.now()).build();
        event.setImages(List.of(img));
        return event;
    }

    @Test
    void getMyCreatedEvents_MapsUnifiedPreviewDtoWithFlags() {
        Long userId = 100L;
        Event ev = buildPlaceEvent(1L, userId, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(2));
        Page<Event> page = new PageImpl<>(List.of(ev), PageRequest.of(0, 10), 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), any(Pageable.class))).thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        EventPreviewDto dto = result.getContent().get(0);
        assertEquals(ev.getId(), dto.getId());
        assertEquals(ev.getTitle(), dto.getTitle());
        assertEquals(EventStatus.UPCOMING, dto.getStatus());
        assertNotNull(dto.getNearestStart());
        assertNotNull(dto.getNearestFinish());
        assertEquals("open", dto.getVisibility());
        assertTrue(dto.isCanEdit());
        assertTrue(dto.isOrganizer());
        assertNotNull(dto.getTypes());
        assertTrue(dto.getTypes().isPlace());
        assertFalse(dto.getTypes().isOnline());
    }

    @Test
    void getMyEvents_PlaceWithCoordinates_ComputesDistanceNullable() {
        Long userId = 200L;
        Event ev = buildPlaceEvent(1L, 999L, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1));
        Page<Event> page = new PageImpl<>(List.of(ev), PageRequest.of(0, 10), 1);

        when(eventAttenderRepo.findJoinedEventsWithSorting(eq(userId), any(), eq(EventType.PLACE.name()), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        Page<EventPreviewDto> result = eventService.getMyEvents(userId, EventType.PLACE, null, 50.1, 30.1, PageRequest.of(0,10));

        EventPreviewDto dto = result.getContent().get(0);
        assertEquals(EventStatus.UPCOMING, dto.getStatus());
        assertNotNull(dto.getNearestStart());
        assertNotNull(dto.getNearestFinish());
        assertEquals("open", dto.getVisibility());
        assertNotNull(dto.getDistance());
        assertFalse(dto.isOrganizer());
    }
}


