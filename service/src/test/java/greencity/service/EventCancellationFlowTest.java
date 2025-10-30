package greencity.service;

import greencity.entity.Event;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import greencity.enums.Role;
import greencity.dto.user.UserVO;
import greencity.exception.exceptions.BadRequestException;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the event cancel join (removeAttender) flow.
 * Tests successful cancellation, edge cases, and business rule enforcement.
 */
@ExtendWith(SpringExtension.class)
class EventCancellationFlowTest {
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
    void cancelJoinUpcomingEvent_Success() {
        Long eventId = 1L;
        Long attendeeId = 100L;
        Long organizerId = 999L;

        UserVO attendee = UserVO.builder().id(attendeeId).role(Role.ROLE_USER).build();
        Event event = buildEvent(eventId, organizerId,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, attendeeId)).thenReturn(true);
        when(eventAttenderRepo.deleteByEventIdAndUserId(eventId, attendeeId)).thenReturn(1);

        boolean result = eventService.removeAttender(eventId, attendee);

        assertTrue(result);
        verify(eventAttenderRepo).deleteByEventIdAndUserId(eventId, attendeeId);
    }

    @Test
    void cancelJoinLiveEvent_Success() {
        Long eventId = 2L;
        Long attendeeId = 200L;
        Long organizerId = 999L;

        UserVO attendee = UserVO.builder().id(attendeeId).role(Role.ROLE_USER).build();
        Event event = buildEvent(eventId, organizerId,
                OffsetDateTime.now().minusHours(1),
                OffsetDateTime.now().plusHours(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, attendeeId)).thenReturn(true);
        when(eventAttenderRepo.deleteByEventIdAndUserId(eventId, attendeeId)).thenReturn(1);

        boolean result = eventService.removeAttender(eventId, attendee);

        assertTrue(result);
        verify(eventAttenderRepo).deleteByEventIdAndUserId(eventId, attendeeId);
    }

    @Test
    void cancelJoinPassedEvent_ThrowsBadRequestException() {
        Long eventId = 3L;
        Long attendeeId = 300L;
        Long organizerId = 999L;

        UserVO attendee = UserVO.builder().id(attendeeId).role(Role.ROLE_USER).build();
        Event event = buildEvent(eventId, organizerId,
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().minusDays(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, attendeeId)).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> eventService.removeAttender(eventId, attendee)
        );

        assertTrue(exception.getMessage().contains("passed"));
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void cancelJoinNotAttender_ReturnsFalse() {
        Long eventId = 4L;
        Long attendeeId = 400L;
        Long organizerId = 999L;

        UserVO attendee = UserVO.builder().id(attendeeId).role(Role.ROLE_USER).build();
        Event event = buildEvent(eventId, organizerId,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, attendeeId)).thenReturn(false);

        boolean result = eventService.removeAttender(eventId, attendee);

        assertFalse(result);
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void cancelJoinEventNotFound_ThrowsNotFoundException() {
        Long eventId = 999L;
        Long attendeeId = 500L;

        UserVO attendee = UserVO.builder().id(attendeeId).role(Role.ROLE_USER).build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(greencity.exception.exceptions.NotFoundException.class,
                () -> eventService.removeAttender(eventId, attendee));

        verify(eventAttenderRepo, never()).existsByEventIdAndUserId(anyLong(), anyLong());
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }
}

