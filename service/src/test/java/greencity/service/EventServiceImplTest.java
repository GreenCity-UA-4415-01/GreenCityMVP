package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.event.*;
import greencity.dto.user.UserVO;
import greencity.entity.Event;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import greencity.enums.EventStatus;
import greencity.enums.EventType;
import greencity.enums.Role;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.UnauthorizedException;
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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class EventServiceImplTest {

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
    private Clock clock;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private EventServiceImpl eventService;

    private final Long mockEventId = 1L;
    private final Long mockOrganizerId = 10L;
    private final Long mockOtherUserId = 20L;
    private final Long mockUserId = 30L;

    private UserVO createUserVO(Long id, Role role) {
        return UserVO.builder()
            .id(id)
            .role(role)
            .build();
    }

    private Event createMinimalEventEntity(Long organizerId, List<String> imageUrls,
        List<EventDateTimeLocation> dates) {
        List<EventImage> mockEventImages = imageUrls.stream()
            .map(url -> EventImage.builder().imagePath(url).build())
            .collect(Collectors.toList());

        List<EventDateTimeLocation> mockDates = dates != null ? dates : Collections.emptyList();

        return Event.builder()
            .id(mockEventId)
            .organizerId(organizerId)
            .title("Test Event")
            .description("Test Description")
            .open(true)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .images(mockEventImages)
            .dateTimeLocations(mockDates)
            .build();
    }

    @Test
    void getEventById_WithUpcomingEvent_ReturnsEventDtoWithUpcomingStatus() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Green City Workshop", result.getTitle());
        assertEquals(EventStatus.UPCOMING, result.getStatus());
        assertNotNull(result.getNearestStart());
        assertNotNull(result.getNearestFinish());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithLiveEvent_ReturnsEventDtoWithLiveStatus() {
        // Given
        Long eventId = 1L;
        Event event = createLiveEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(EventStatus.LIVE, result.getStatus());
        assertNotNull(result.getNearestStart());
        assertNotNull(result.getNearestFinish());

        // Verify the nearestStart is for a live occurrence (in the past)
        assertTrue(result.getNearestStart().isBefore(OffsetDateTime.now()) ||
            result.getNearestStart().isEqual(OffsetDateTime.now()));
        assertTrue(result.getNearestFinish().isAfter(OffsetDateTime.now()) ||
            result.getNearestFinish().isEqual(OffsetDateTime.now()));
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithPassedEvent_ReturnsEventDtoWithPassedStatus() {
        // Given
        Long eventId = 1L;
        Event event = createPassedEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(EventStatus.PASSED, result.getStatus());
        assertNull(result.getNearestStart());
        assertNull(result.getNearestFinish());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithMultipleOccurrences_ReturnsCorrectNearestDates() {
        // Given
        Long eventId = 1L;
        Event event = createEventWithMultipleOccurrences();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getDatesLocations().size());

        // Should return UPCOMING with the earliest future date
        if (result.getStatus() == EventStatus.UPCOMING) {
            // Find the earliest start date from date locations
            OffsetDateTime earliestStart = event.getDateTimeLocations().stream()
                .map(EventDateTimeLocation::getStartDate)
                .filter(d -> d.isAfter(OffsetDateTime.now()))
                .min(OffsetDateTime::compareTo)
                .orElse(null);

            assertEquals(earliestStart, result.getNearestStart());
        }
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithNonExistentId_ThrowsNotFoundException() {
        // Given
        Long eventId = 999L;

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> eventService.getEventById(eventId));

        assertTrue(exception.getMessage().contains("Event with id " + eventId + " not found"));
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_ReturnsAllEventFields() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(event.getId(), result.getId());
        assertEquals(event.getTitle(), result.getTitle());
        assertEquals(event.getDescription(), result.getDescription());
        assertEquals(event.isOpen(), result.isOpen());
        assertEquals(event.getOrganizerId(), result.getOrganizerId());
        assertNotNull(result.getTitleImage());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getDatesLocations());
        assertNotNull(result.getImageUrls());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithMainImage_ReturnsTitleImage() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result.getTitleImage());
        assertEquals("https://example.com/image1.jpg", result.getTitleImage());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithMultipleImages_ReturnsAllImageUrls() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result.getImageUrls());
        assertEquals(2, result.getImageUrls().size());
        assertTrue(result.getImageUrls().contains("https://example.com/image1.jpg"));
        assertTrue(result.getImageUrls().contains("https://example.com/image2.jpg"));
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_VerifyRepositoryCalledOnce() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        eventService.getEventById(eventId);

        // Then
        verify(eventRepository, times(1)).findById(eventId);
        verifyNoMoreInteractions(eventRepository);
    }

    // Helper methods to create test events
    private Event createUpcomingEvent() {
        Event event = Event.builder()
            .id(1L)
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .organizerId(1L)
            .titleImage("https://example.com/image.jpg")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        // Add upcoming date locations
        List<EventDateTimeLocation> dateLocations = Arrays.asList(
            EventDateTimeLocation.builder()
                .id(1L)
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build());

        List<EventImage> images = Arrays.asList(
            EventImage.builder()
                .id(1L)
                .event(event)
                .imagePath("https://example.com/image1.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now())
                .build(),
            EventImage.builder()
                .id(2L)
                .event(event)
                .imagePath("https://example.com/image2.jpg")
                .main(false)
                .createdAt(OffsetDateTime.now())
                .build());

        event.setDateTimeLocations(dateLocations);
        event.setImages(images);

        return event;
    }

    private Event createLiveEvent() {
        Event event = Event.builder()
            .id(1L)
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .organizerId(1L)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        // Add live date location (currently happening)
        List<EventDateTimeLocation> dateLocations = Arrays.asList(
            EventDateTimeLocation.builder()
                .id(1L)
                .event(event)
                .startDate(OffsetDateTime.now().minusHours(1))
                .finishDate(OffsetDateTime.now().plusHours(1))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build());

        List<EventImage> images = Arrays.asList(
            EventImage.builder()
                .id(1L)
                .event(event)
                .imagePath("https://example.com/image1.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now())
                .build());

        event.setDateTimeLocations(dateLocations);
        event.setImages(images);

        return event;
    }

    private Event createPassedEvent() {
        Event event = Event.builder()
            .id(1L)
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .organizerId(1L)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        // Add passed date location
        List<EventDateTimeLocation> dateLocations = Arrays.asList(
            EventDateTimeLocation.builder()
                .id(1L)
                .event(event)
                .startDate(OffsetDateTime.now().minusDays(2))
                .finishDate(OffsetDateTime.now().minusDays(1))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build());

        List<EventImage> images = Arrays.asList(
            EventImage.builder()
                .id(1L)
                .event(event)
                .imagePath("https://example.com/image1.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now())
                .build());

        event.setDateTimeLocations(dateLocations);
        event.setImages(images);

        return event;
    }

    private Event createEventWithMultipleOccurrences() {
        Event event = Event.builder()
            .id(1L)
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .organizerId(1L)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        // Add multiple date locations
        List<EventDateTimeLocation> dateLocations = Arrays.asList(
            EventDateTimeLocation.builder()
                .id(1L)
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build(),
            EventDateTimeLocation.builder()
                .id(2L)
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(3))
                .finishDate(OffsetDateTime.now().plusDays(3).plusHours(2))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build());

        List<EventImage> images = Arrays.asList(
            EventImage.builder()
                .id(1L)
                .event(event)
                .imagePath("https://example.com/image1.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now())
                .build());

        event.setDateTimeLocations(dateLocations);
        event.setImages(images);

        return event;
    }

    @Test
    void deleteEvent_AsOrganizer_Success() {
        UserVO organizer = createUserVO(mockOrganizerId, Role.ROLE_USER);
        List<String> imageUrls = Arrays.asList("path/to/img1.jpg", "path/to/img2.png");

        Event mockEventEntity = createMinimalEventEntity(mockOrganizerId, imageUrls,
            Collections.singletonList(
                EventDateTimeLocation.builder()
                    .startDate(OffsetDateTime.now().plusDays(1))
                    .finishDate(OffsetDateTime.now().plusDays(2))
                    .build()));

        when(eventRepository.findById(mockEventId)).thenReturn(Optional.of(mockEventEntity));

        eventService.deleteEvent(mockEventId, organizer);

        verify(eventRepository).findById(mockEventId);

        imageUrls.forEach(url -> verify(imageStorageService).deleteImage(url));
        verify(eventImageRepository).deleteAllByEventId(mockEventId);
        verify(dateTimeLocationRepository).deleteAllByEventId(mockEventId);

        verify(entityManager).flush();
        verify(entityManager).clear();

        verify(eventRepository).deleteById(mockEventId);
    }

    @Test
    void deleteEvent_AsAdmin_Success() {
        UserVO admin = createUserVO(mockOtherUserId, Role.ROLE_ADMIN);
        List<String> imageUrls = Collections.emptyList();

        Event mockEventEntity = createMinimalEventEntity(mockOrganizerId, imageUrls,
            Collections.singletonList(
                EventDateTimeLocation.builder()
                    .startDate(OffsetDateTime.now().plusDays(1))
                    .finishDate(OffsetDateTime.now().plusDays(2))
                    .build()));

        when(eventRepository.findById(mockEventId)).thenReturn(Optional.of(mockEventEntity));

        eventService.deleteEvent(mockEventId, admin);

        verify(eventRepository).findById(mockEventId);

        verify(imageStorageService, never()).deleteImage(anyString());
        verify(eventImageRepository).deleteAllByEventId(mockEventId);
        verify(dateTimeLocationRepository).deleteAllByEventId(mockEventId);

        verify(entityManager).flush();
        verify(entityManager).clear();

        verify(eventRepository).deleteById(mockEventId);
    }

    @Test
    void deleteEvent_EventHasEmptyImages_Success() {
        UserVO organizer = createUserVO(mockOrganizerId, Role.ROLE_USER);
        List<String> imageUrls = Collections.emptyList();

        Event mockEventEntity = createMinimalEventEntity(mockOrganizerId, imageUrls,
            Collections.singletonList(
                EventDateTimeLocation.builder()
                    .startDate(OffsetDateTime.now().plusDays(1))
                    .finishDate(OffsetDateTime.now().plusDays(2))
                    .build()));

        when(eventRepository.findById(mockEventId)).thenReturn(Optional.of(mockEventEntity));

        eventService.deleteEvent(mockEventId, organizer);

        verify(eventRepository).findById(mockEventId);

        verify(imageStorageService, never()).deleteImage(anyString());
        verify(eventImageRepository).deleteAllByEventId(mockEventId);
        verify(dateTimeLocationRepository).deleteAllByEventId(mockEventId);

        verify(entityManager).flush();
        verify(entityManager).clear();

        verify(eventRepository).deleteById(mockEventId);
    }

    @Test
    void deleteEvent_UnauthorizedException_NotOrganizerAndNotAdmin() {
        UserVO regularUser = createUserVO(mockOtherUserId, Role.ROLE_USER);
        List<String> imageUrls = Collections.emptyList();

        Event mockEventEntity = createMinimalEventEntity(mockOrganizerId, imageUrls,
            Collections.singletonList(
                EventDateTimeLocation.builder()
                    .startDate(OffsetDateTime.now().plusDays(1))
                    .finishDate(OffsetDateTime.now().plusDays(2))
                    .build()));

        when(eventRepository.findById(mockEventId)).thenReturn(Optional.of(mockEventEntity));

        UnauthorizedException thrown = assertThrows(UnauthorizedException.class,
            () -> eventService.deleteEvent(mockEventId, regularUser),
            "Expected UnauthorizedException to be thrown");

        assertEquals(ErrorMessage.USER_HAS_NO_PERMISSION, thrown.getMessage());

        verify(eventRepository).findById(mockEventId);
        verify(eventRepository, never()).deleteById(anyLong());
        verify(eventImageRepository, never()).deleteAllByEventId(anyLong());
        verify(dateTimeLocationRepository, never()).deleteAllByEventId(anyLong());

        verify(entityManager, never()).flush();
        verify(entityManager, never()).clear();
    }

    @Test
    void deleteEvent_NotFoundException() {
        UserVO user = createUserVO(mockOrganizerId, Role.ROLE_USER);

        when(eventRepository.findById(mockEventId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> eventService.deleteEvent(mockEventId, user),
            "Expected NotFoundException to be thrown when event not found");

        verify(eventRepository).findById(mockEventId);

        verify(entityManager, never()).flush();
        verify(entityManager, never()).clear();
    }

    @Test
    void findById_WithExistingEvent_ReturnsEventDto() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        EventDto result = eventService.findById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals(event.getTitle(), result.getTitle());
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void findById_WithNonExistentEvent_ThrowsNotFoundException() {
        // Given
        Long eventId = 999L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class,
                () -> eventService.findById(eventId));
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void getVisibleEvents_ReturnsOpenEvents() {
        // Given
        Event openEvent = createUpcomingEvent();
        openEvent.setOpen(true);

        Event closedEvent = createUpcomingEvent();
        closedEvent.setId(2L);
        closedEvent.setOrganizerId(999L);
        closedEvent.setOpen(false);

        when(eventRepository.findAll()).thenReturn(Arrays.asList(openEvent, closedEvent));

        UserVO user = createUserVO(mockOrganizerId, Role.ROLE_USER);

        // When
        List<EventDto> result = eventService.getVisibleEvents(user);

        // Then
        assertNotNull(result);
        // At least the open event should be visible
        assertTrue(result.size() >= 1);
    }

    @Test
    void addAttender_WithValidData_ReturnsTrue() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();
        UserVO user = createUserVO(mockUserId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, mockUserId)).thenReturn(false);
        when(eventAttenderRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        boolean result = eventService.addAttender(eventId, user);

        // Then
        assertTrue(result);
        verify(eventAttenderRepo).existsByEventIdAndUserId(eventId, mockUserId);
        verify(eventAttenderRepo).save(any());
    }

    @Test
    void addAttender_WhenAlreadyAttender_ReturnsFalse() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();
        UserVO user = createUserVO(mockUserId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, mockUserId)).thenReturn(true);

        // When
        boolean result = eventService.addAttender(eventId, user);

        // Then
        assertFalse(result);
        verify(eventAttenderRepo, never()).save(any());
    }

    @Test
    void addAttender_WithNonExistentEvent_ThrowsNotFoundException() {
        // Given
        Long eventId = 999L;
        UserVO user = createUserVO(mockUserId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class,
                () -> eventService.addAttender(eventId, user));
    }

    @Test
    void removeAttender_WithUpcomingEvent_ReturnsTrue() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();
        UserVO user = createUserVO(mockUserId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, mockUserId)).thenReturn(true);
        when(eventAttenderRepo.deleteByEventIdAndUserId(eventId, mockUserId)).thenReturn(1);

        // When
        boolean result = eventService.removeAttender(eventId, user);

        // Then
        assertTrue(result);
        verify(eventAttenderRepo).deleteByEventIdAndUserId(eventId, mockUserId);
    }

    @Test
    void removeAttender_WithPassedEvent_ThrowsBadRequestException() {
        // Given
        Long eventId = 1L;
        Event event = createPassedEvent();
        UserVO user = createUserVO(mockUserId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, mockUserId)).thenReturn(true);

        // When & Then
        greencity.exception.exceptions.BadRequestException exception =
                assertThrows(greencity.exception.exceptions.BadRequestException.class,
                        () -> eventService.removeAttender(eventId, user));

        assertTrue(exception.getMessage().contains("passed"));
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void removeAttender_WhenNotAttender_ReturnsFalse() {
        // Given
        Long eventId = 1L;
        Event event = createUpcomingEvent();
        UserVO user = createUserVO(mockUserId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, mockUserId)).thenReturn(false);

        // When
        boolean result = eventService.removeAttender(eventId, user);

        // Then
        assertFalse(result);
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void removeAttender_WithNonExistentEvent_ThrowsNotFoundException() {
        // Given
        Long eventId = 999L;
        UserVO user = createUserVO(mockUserId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class,
                () -> eventService.removeAttender(eventId, user));
    }

    @Test
    void getMyEvents_WithPagination_ReturnsPagedResults() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Event> events = List.of(createUpcomingEvent());

        when(eventAttenderRepo.findJoinedEventsDefaultSorting(eq(userId), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(events, pageable, 1));
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getMyEvents(userId, null, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getMyEvents_WithEventTypeFilter_ReturnsFilteredEvents() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Event> events = List.of(createUpcomingEvent());

        when(eventAttenderRepo.findJoinedEventsWithSorting(
                eq(userId), any(OffsetDateTime.class), eq(EventType.PLACE.name()), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(events, pageable, 1));
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getMyEvents(userId, EventType.PLACE, null, 50.0, 30.0, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyCreatedEvents_ReturnsCreatedEventsWithCanEditTrue() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        Event event = createUpcomingEvent();
        event.setOrganizerId(userId);
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(event), pageable, 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), eq(pageable)))
                .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        EventPreviewDto dto = result.getContent().get(0);
        assertTrue(dto.isCanEdit());
        assertTrue(dto.isOrganizer());
    }

    @Test
    void getMyCreatedEvents_WithStatusFilter_ReturnsFilteredEvents() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        Event event = createUpcomingEvent();
        event.setOrganizerId(userId);
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(event), pageable, 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(eq(userId), eq(pageable)))
                .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, EventStatus.UPCOMING, pageable);

        // Then
        assertNotNull(result);
        result.getContent().forEach(dto ->
                assertEquals(EventStatus.UPCOMING, dto.getStatus())
        );
    }

    @Test
    void getRelatedEvents_ReturnsUnionOfCreatedAndJoined() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        Event event = createUpcomingEvent();
        event.setOrganizerId(userId);
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(event), pageable, 1);

        when(eventRepository.findRelatedEventsByUserId(eq(userId), eq(pageable)))
                .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getRelatedEvents(userId, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository).findRelatedEventsByUserId(eq(userId), eq(pageable));
    }
}
