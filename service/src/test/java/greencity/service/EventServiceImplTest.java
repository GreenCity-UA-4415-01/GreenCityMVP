package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.event.*;
import greencity.dto.user.UserVO;
import greencity.entity.Event;
import greencity.entity.EventAttender;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import greencity.enums.EventStatus;
import greencity.enums.EventType;
import greencity.enums.Role;
import greencity.exception.exceptions.BadRequestException;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.PastEventUpdateException;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class EventServiceImplTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(OffsetDateTime.now().toInstant(), ZoneOffset.UTC);

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
            .toList();

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

    private EventDto createEventDtoFromEvent(Event event) {
        return EventDto.builder()
            .id(event.getId())
            .title(event.getTitle())
            .description(event.getDescription())
            .status(EventStatus.UPCOMING) // Assuming upcoming
            .build();
    }

    private EventPreviewDto createEventPreviewDtoFromEvent(Event event) {
        return EventPreviewDto.builder()
            .id(event.getId())
            .title(event.getTitle())
            .status(EventStatus.UPCOMING) // Assuming upcoming
            .build();
    }

    private EventDto createMockEventDto(Long id) {
        OffsetDateTime nearestStart = OffsetDateTime.now(FIXED_CLOCK).plusDays(1);
        OffsetDateTime nearestFinish = OffsetDateTime.now(FIXED_CLOCK).plusDays(1).plusHours(2);
        OffsetDateTime creationTime = OffsetDateTime.now(FIXED_CLOCK);

        EventDateLocationDto dateLocationDto = EventDateLocationDto.builder()
            .startDate(nearestStart)
            .finishDate(nearestFinish)
            .latitude(50.4501)
            .longitude(30.5234)
            .onlineLink(null)
            .build();

        // The image entity had a path "http://example.com/image1.jpg"
        List<String> imageUrls = Collections.singletonList("http://example.com/image1.jpg");

        return EventDto.builder()
            .id(id)
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .organizerId(1L)
            .titleImage("http://example.com/title.jpg")
            .createdAt(creationTime) // NEW
            .updatedAt(creationTime) // NEW
            .datesLocations(Collections.singletonList(dateLocationDto))
            .imageUrls(imageUrls) // CHANGED from additionalImages
            .status(EventStatus.UPCOMING)
            .nearestStart(nearestStart) // NEW
            .nearestFinish(nearestFinish) // NEW
            .build();
    }

    private Event createUpcomingEvent() {
        Event event = Event.builder()
            .id(1L)
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .organizerId(10L)
            .titleImage("https://example.com/image.jpg")
            .createdAt(OffsetDateTime.now().minusDays(1)) // Use minusDays(1) for consistent test context
            .updatedAt(OffsetDateTime.now().minusDays(1)) // Use minusDays(1) for consistent test context
            .build();

        // Add upcoming date locations
        List<EventDateTimeLocation> dateLocations = new ArrayList<>(Collections.singletonList(
            EventDateTimeLocation.builder()
                .id(1L)
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build()));

        List<EventImage> images = new ArrayList<>(Arrays.asList(
            EventImage.builder()
                .id(1L)
                .event(event)
                .imagePath("https://example.com/image1.jpg")
                .main(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build(),
            EventImage.builder()
                .id(2L)
                .event(event)
                .imagePath("https://example.com/image2.jpg")
                .main(false)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build()));

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
        List<EventDateTimeLocation> dateLocations = Collections.singletonList(
            EventDateTimeLocation.builder()
                .id(1L)
                .event(event)
                .startDate(OffsetDateTime.now().minusHours(1))
                .finishDate(OffsetDateTime.now().plusHours(1))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build());

        List<EventImage> images = Collections.singletonList(
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
        List<EventDateTimeLocation> dateLocations = Collections.singletonList(
            EventDateTimeLocation.builder()
                .id(1L)
                .event(event)
                .startDate(OffsetDateTime.now().minusDays(2))
                .finishDate(OffsetDateTime.now().minusDays(1))
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build());

        List<EventImage> images = Collections.singletonList(
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

        List<EventImage> images = Collections.singletonList(
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

    private Event createEvent() {
        EventDateTimeLocation location = EventDateTimeLocation.builder()
            .id(1L)
            .startDate(OffsetDateTime.now(FIXED_CLOCK).plusDays(1))
            .finishDate(OffsetDateTime.now(FIXED_CLOCK).plusDays(1).plusHours(2))
            .latitude(50.4501)
            .longitude(30.5234)
            .onlineLink(null)
            .createdAt(OffsetDateTime.now(FIXED_CLOCK))
            .updatedAt(OffsetDateTime.now(FIXED_CLOCK))
            .build();

        EventImage image = EventImage.builder()
            .id(1L)
            .imagePath("https://example.com/image1.jpg")
            .main(false)
            .createdAt(OffsetDateTime.now(FIXED_CLOCK))
            .build();

        Event event = Event.builder()
            .id(1L)
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .organizerId(1L)
            .titleImage(null)
            .createdAt(OffsetDateTime.now(FIXED_CLOCK))
            .updatedAt(OffsetDateTime.now(FIXED_CLOCK))
            .dateTimeLocations(new ArrayList<>(Collections.singletonList(location)))
            .images(new ArrayList<>(Collections.singletonList(image)))
            .build();

        location.setEvent(event);
        image.setEvent(event);

        return event;
    }

    private AddEventDtoRequest createAddEventDtoRequest() {
        EventDateLocationDto dateLocationDto = EventDateLocationDto.builder()
            .startDate(OffsetDateTime.now(FIXED_CLOCK).plusDays(1))
            .finishDate(OffsetDateTime.now(FIXED_CLOCK).plusDays(1).plusHours(2))
            .latitude(50.4501)
            .longitude(30.5234)
            .onlineLink(null)
            .build();

        return AddEventDtoRequest.builder()
            .title("Green City Workshop")
            .description("Learn about sustainable living practices")
            .open(true)
            .datesLocations(Collections.singletonList(dateLocationDto))
            .build();
    }

    private MultipartFile createMockMultipartFile() {
        return new MockMultipartFile(
            "file",
            "filename.jpg",
            "image/jpeg",
            "some-image-bytes".getBytes());
    }

    @Test
    void createEvent_Success_WithTitleImage() {
        // Given
        AddEventDtoRequest createEventDto = createAddEventDtoRequest();
        UserVO user = createUserVO(1L, Role.ROLE_USER);
        MultipartFile titleImage = createMockMultipartFile();
        String titleImagePath = "http://example.com/title.jpg"; // Mocked path set by image handler

        Event eventToSave = createEvent();
        eventToSave.setId(null);
        eventToSave.setOrganizerId(user.getId());
        eventToSave.setTitleImage(null); // Title image path is set *after* save/upload logic

        Event savedEvent = createEvent();
        savedEvent.setId(1L);
        savedEvent.setOrganizerId(user.getId());
        savedEvent.setTitleImage(titleImagePath);
        savedEvent.getDateTimeLocations().forEach(l -> l.setEvent(savedEvent));
        savedEvent.getImages().forEach(i -> i.setEvent(savedEvent));

        EventDto expectedEventDto = createMockEventDto(1L);
        expectedEventDto.setTitleImage(titleImagePath);

        when(mapper.map(createEventDto, Event.class)).thenReturn(eventToSave);

        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        when(dateTimeLocationRepository.saveAll(anyList())).thenReturn(savedEvent.getDateTimeLocations());
        when(eventImageRepository.saveAll(anyList())).thenReturn(savedEvent.getImages());

        when(mapper.map(any(Event.class), eq(EventDto.class))).thenReturn(expectedEventDto);

        // When
        EventDto actualEventDto =
            eventService.createEvent(createEventDto, new MultipartFile[] {titleImage}, user.getId());

        // Then
        assertNotNull(actualEventDto);
        assertAll("EventDto creation success with title image",
            () -> assertEquals(expectedEventDto.getId(), actualEventDto.getId(), "Event ID should match"),
            () -> assertEquals(expectedEventDto.getTitle(), actualEventDto.getTitle(), "Title should match"),
            () -> assertEquals(expectedEventDto.getDescription(), actualEventDto.getDescription(),
                "Description should match"),
            () -> assertEquals(expectedEventDto.getOrganizerId(), actualEventDto.getOrganizerId(),
                "Organizer ID should match"),
            () -> assertEquals(expectedEventDto.getCreatedAt(), actualEventDto.getCreatedAt(),
                "CreatedAt should match"),
            () -> assertEquals(expectedEventDto.getUpdatedAt(), actualEventDto.getUpdatedAt(),
                "UpdatedAt should match"),
            () -> assertEquals(expectedEventDto.getNearestStart(), actualEventDto.getNearestStart(),
                "NearestStart should match"),
            () -> assertEquals(expectedEventDto.getNearestFinish(), actualEventDto.getNearestFinish(),
                "NearestFinish should match"),
            () -> assertEquals(expectedEventDto.getDatesLocations().size(), actualEventDto.getDatesLocations().size(),
                "Date/Location count should match"));

        // Verification
        verify(eventRepository).save(any(Event.class));
        verify(dateTimeLocationRepository).saveAll(anyList());
        verify(eventImageRepository).saveAll(anyList());
    }

    @Test
    void createEvent_Success_WithoutTitleImage() {
        // Given
        AddEventDtoRequest createEventDto = createAddEventDtoRequest();
        UserVO user = createUserVO(1L, Role.ROLE_USER);
        // No title image file, so we will pass an empty array of MultipartFile

        Event eventToSave = createEvent();
        eventToSave.setId(null);
        eventToSave.setOrganizerId(user.getId());
        eventToSave.setTitleImage(null); // Ensure null title image path

        Event savedEvent = createEvent();
        savedEvent.setId(2L);
        savedEvent.setOrganizerId(user.getId());
        savedEvent.setTitleImage(null); // Ensure null title image path in saved entity
        // Ensure image and location links are set to the saved event
        savedEvent.getDateTimeLocations().forEach(l -> l.setEvent(savedEvent));
        savedEvent.getImages().forEach(i -> i.setEvent(savedEvent));

        // Use the updated helper signature
        EventDto expectedEventDto = createMockEventDto(2L);
        // Explicitly set to null to match the 'WithoutTitleImage' scenario
        expectedEventDto.setTitleImage(null);

        // Mocking behavior
        when(mapper.map(createEventDto, Event.class)).thenReturn(eventToSave);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Mock saving of related entities
        when(dateTimeLocationRepository.saveAll(anyList())).thenReturn(savedEvent.getDateTimeLocations());
        when(eventImageRepository.saveAll(anyList())).thenReturn(savedEvent.getImages());

        // FIX: Change mapper mock from specific object to any(Event.class)
        when(mapper.map(any(Event.class), eq(EventDto.class))).thenReturn(expectedEventDto);

        // When
        // Updated call: passes AddEventDtoRequest, empty MultipartFile array, and
        // organizerId
        EventDto actualEventDto = eventService.createEvent(createEventDto, new MultipartFile[] {}, user.getId());

        // Then
        assertNotNull(actualEventDto);
        // Using assertAll for field-by-field comparison
        assertAll("EventDto creation success without title image",
            () -> assertEquals(expectedEventDto.getId(), actualEventDto.getId(), "Event ID should match"),
            () -> assertEquals(expectedEventDto.getTitle(), actualEventDto.getTitle(), "Title should match"),
            // Assert against the expected null value
            () -> assertNull(actualEventDto.getTitleImage(), "Title Image path should be null"),
            () -> assertEquals(expectedEventDto.getCreatedAt(), actualEventDto.getCreatedAt(),
                "CreatedAt should match"),
            () -> assertEquals(expectedEventDto.getUpdatedAt(), actualEventDto.getUpdatedAt(),
                "UpdatedAt should match"),
            () -> assertEquals(expectedEventDto.getNearestStart(), actualEventDto.getNearestStart(),
                "NearestStart should match"),
            () -> assertEquals(expectedEventDto.getNearestFinish(), actualEventDto.getNearestFinish(),
                "NearestFinish should match"),
            () -> assertEquals(expectedEventDto.getDatesLocations().size(), actualEventDto.getDatesLocations().size(),
                "Date/Location count should match"));

        // Verification
        verify(eventRepository).save(any(Event.class));
        verify(dateTimeLocationRepository).saveAll(anyList());
        verify(eventImageRepository).saveAll(anyList());
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
        Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(userId, pageable))
            .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        EventPreviewDto dto = result.getContent().getFirst();
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
        Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(userId, pageable))
            .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, EventStatus.UPCOMING, pageable);

        // Then
        assertNotNull(result);
        result.getContent().forEach(dto -> assertEquals(EventStatus.UPCOMING, dto.getStatus()));
    }

    @Test
    void getRelatedEvents_ReturnsUnionOfCreatedAndJoined() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        Event event = createUpcomingEvent();
        event.setOrganizerId(userId);
        Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);

        when(eventRepository.findRelatedEventsByUserId(userId, pageable))
            .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());

        // When
        Page<EventPreviewDto> result = eventService.getRelatedEvents(userId, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository).findRelatedEventsByUserId(userId, pageable);
    }

    @Test
    void getMyCreatedEvents_ReturnsUpcomingEvents_WhenStatusUpcoming() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        Event event = createUpcomingEvent();
        event.setOrganizerId(userId);
        Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(userId, pageable))
            .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());
        when(mapper.map(any(Event.class), eq(EventPreviewDto.class)))
            .thenReturn(createEventPreviewDtoFromEvent(event));

        // When
        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, EventStatus.UPCOMING, pageable);

        // Then
        assertNotNull(result);
        result.getContent().forEach(dto -> assertEquals(EventStatus.UPCOMING, dto.getStatus()));
    }

    @Test
    void updateEvent_Success_AsOrganizer() {
        Long eventId = 1L;
        UserVO organizer = createUserVO(10L, Role.ROLE_USER);
        Event existingEvent = createUpcomingEvent();
        existingEvent.setOrganizerId(organizer.getId());

        UpdateEventDtoRequest updateDto = UpdateEventDtoRequest.builder()
            .title("Updated Title")
            .description("Updated Description Content")
            .datesLocations(Collections.emptyList())
            .build();

        MultipartFile mockImage = mock(MultipartFile.class);
        MultipartFile[] newImages = {mockImage};
        List<String> newImagePaths = List.of("new/path/3.jpg");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        when(userService.findById(organizer.getId())).thenReturn(organizer);

        when(imageStorageService.deleteImage(anyString())).thenReturn(true);

        when(imageStorageService.storeImages(newImages, eventId)).thenReturn(newImagePaths);

        when(mapper.map(any(Event.class), eq(EventDto.class))).thenReturn(createMockEventDto(eventId));

        EventDto result = eventService.updateEvent(eventId, updateDto, newImages, organizer.getId());

        assertNotNull(result);
        verify(eventRepository, times(1)).findById(eventId);

        verify(imageStorageService, atLeastOnce()).deleteImage(anyString());

        verify(imageStorageService, times(1)).storeImages(newImages, eventId);

        verify(dateTimeLocationRepository, never()).deleteAll(anyList());
        verify(eventImageRepository, never()).deleteAll(anyList());
    }

    @Test
    void updateEvent_ThrowsPastEventUpdateException() {
        Long eventId = 2L;
        Long callingUserId = 10L;
        Event pastEvent = createPassedEvent();
        pastEvent.setOrganizerId(callingUserId);
        UpdateEventDtoRequest updateDto =
            UpdateEventDtoRequest.builder().title("Try to Update").datesLocations(Collections.emptyList()).build();

        UserVO callingUser = createUserVO(callingUserId, Role.ROLE_USER);
        when(userService.findById(callingUserId)).thenReturn(callingUser);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pastEvent));

        assertThrows(PastEventUpdateException.class,
            () -> eventService.updateEvent(eventId, updateDto, null, callingUserId));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_ThrowsUnauthorizedException_WrongUser() {
        Long eventId = 1L;
        Long callingUserId = 99L;

        Event existingEvent = createUpcomingEvent();
        existingEvent.setOrganizerId(10L);

        UpdateEventDtoRequest updateDto = UpdateEventDtoRequest.builder().title("Try to Update").build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

        UserVO wrongUser = createUserVO(callingUserId, Role.ROLE_USER);
        when(userService.findById(callingUserId)).thenReturn(wrongUser);

        assertThrows(UnauthorizedException.class,
            () -> eventService.updateEvent(eventId, updateDto, null, callingUserId));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void deleteEvent_Success_AsAdmin() {
        Long eventId = 1L;
        UserVO adminUser = createUserVO(99L, Role.ROLE_ADMIN);
        Event existingEvent = createUpcomingEvent();
        existingEvent.setOrganizerId(10L);

        existingEvent.setImages(Collections.singletonList(
            EventImage.builder().imagePath("path/to/img.png").build()));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        when(imageStorageService.deleteImage(anyString())).thenReturn(true);

        doNothing().when(eventRepository).deleteById(eventId);

        eventService.deleteEvent(eventId, adminUser);

        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, times(1)).deleteById(eventId);

        verify(imageStorageService, times(1)).deleteImage("path/to/img.png");
    }

    @Test
    void deleteEvent_ThrowsUnauthorizedException_WrongUser() {
        Long eventId = 1L;
        UserVO wrongUser = createUserVO(99L, Role.ROLE_USER);
        Event existingEvent = createUpcomingEvent();
        existingEvent.setOrganizerId(10L); // Organizer is 10L

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

        assertThrows(UnauthorizedException.class, () -> eventService.deleteEvent(eventId, wrongUser));
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void getEventById_Success() {
        Long eventId = 1L;
        Event event = createUpcomingEvent();
        EventDto expectedDto = createMockEventDto(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        EventDto result = eventService.getEventById(eventId);

        assertNotNull(result);
        assertEquals(expectedDto.getId(), result.getId());
        assertEquals(expectedDto.getTitle(), result.getTitle());

        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void getEventsByTitle_Success() {
        String titlePart = "Upcom";
        Event event = createUpcomingEvent();
        List<Event> events = Collections.singletonList(event);
        when(eventRepository.findByTitleContainingIgnoreCase(titlePart)).thenReturn(events);
        when(mapper.map(any(Event.class), eq(EventPreviewDto.class))).thenReturn(createEventPreviewDtoFromEvent(event));

        List<EventPreviewDto> result = eventService.searchEventsByTitle(titlePart);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findByTitleContainingIgnoreCase(titlePart);
    }

    @Test
    void addAttender_Success() {
        Long eventId = 1L;
        UserVO user = createUserVO(5L, Role.ROLE_USER);
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, user.getId())).thenReturn(false);
        when(eventAttenderRepo.save(any(EventAttender.class))).thenReturn(new EventAttender());

        boolean result = eventService.addAttender(eventId, user);

        assertTrue(result);
        verify(eventAttenderRepo, times(1)).existsByEventIdAndUserId(eventId, user.getId());
        verify(eventAttenderRepo, times(1)).save(any(EventAttender.class));
    }

    @Test
    void addAttender_AlreadyAttender_ReturnsFalse() {
        Long eventId = 1L;
        UserVO user = createUserVO(5L, Role.ROLE_USER);
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, user.getId())).thenReturn(true);

        boolean result = eventService.addAttender(eventId, user);

        assertFalse(result);
        verify(eventAttenderRepo, times(1)).existsByEventIdAndUserId(eventId, user.getId());
        verify(eventAttenderRepo, never()).save(any(EventAttender.class));
    }

    @Test
    void removeAttender_Success() {
        // Given
        Long eventId = 1L;
        UserVO user = createUserVO(20L, Role.ROLE_USER);
        Event event = createUpcomingEvent(); // Status is Upcoming
        EventDto eventDto = createMockEventDto(eventId);
        eventDto.setStatus(EventStatus.UPCOMING);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, user.getId())).thenReturn(true);
        when(eventAttenderRepo.deleteByEventIdAndUserId(eventId, user.getId())).thenReturn(1);
        when(mapper.map(event, EventDto.class)).thenReturn(eventDto); // Mock toEventDto

        // When
        boolean result = eventService.removeAttender(eventId, user);

        // Then
        assertTrue(result);
        verify(eventAttenderRepo, times(1)).deleteByEventIdAndUserId(eventId, user.getId());
    }

    @Test
    void removeAttender_ThrowsBadRequestException_ForPastEvent() {
        Long eventId = 2L;
        UserVO user = createUserVO(5L, Role.ROLE_USER);
        Event event = createPassedEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        EventDto passedEventDto = createEventDtoFromEvent(event);
        passedEventDto.setStatus(EventStatus.PASSED);
        when(mapper.map(any(Event.class), eq(EventDto.class))).thenReturn(passedEventDto);

        assertThrows(BadRequestException.class, () -> eventService.removeAttender(eventId, user));
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void removeAttender_NotAttender_ReturnsFalse() {
        Long eventId = 1L;
        UserVO user = createUserVO(5L, Role.ROLE_USER);
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, user.getId())).thenReturn(false);

        boolean result = eventService.removeAttender(eventId, user);

        assertFalse(result);
        verify(eventAttenderRepo, times(1)).existsByEventIdAndUserId(eventId, user.getId());
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void getMyCreatedEvents_ReturnsPastEvents_WhenStatusPast() {
        Long userId = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        Event event = createPassedEvent();
        event.setOrganizerId(userId);
        Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);

        when(eventRepository.findByOrganizerIdOrderByNearestStart(userId, pageable))
            .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());
        EventPreviewDto pastDto = createEventPreviewDtoFromEvent(event);
        pastDto.setStatus(EventStatus.PASSED);
        when(mapper.map(any(Event.class), eq(EventPreviewDto.class)))
            .thenReturn(pastDto);

        Page<EventPreviewDto> result = eventService.getMyCreatedEvents(userId, EventStatus.PASSED, pageable);

        assertNotNull(result);
        result.getContent().forEach(dto -> assertEquals(EventStatus.PASSED, dto.getStatus()));
    }

    @Test
    void getRelatedEvents_ReturnsUpcomingEvents_WhenStatusUpcoming() {
        // Given
        Long userId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        Event event = createUpcomingEvent();
        event.setOrganizerId(userId);
        Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);

        when(eventRepository.findRelatedEventsByUserId(userId, pageable))
            .thenReturn(eventPage);
        when(userService.findById(userId)).thenReturn(UserVO.builder().id(userId).role(Role.ROLE_USER).build());
        when(mapper.map(any(Event.class), eq(EventPreviewDto.class)))
            .thenReturn(createEventPreviewDtoFromEvent(event));

        // When
        Page<EventPreviewDto> result = eventService.getRelatedEvents(userId, EventStatus.UPCOMING, pageable);

        // Then
        assertNotNull(result);
        result.getContent().forEach(dto -> assertEquals(EventStatus.UPCOMING, dto.getStatus()));
    }

    @Test
    void deleteEvent_Success_AsOrganizer() {
        // Given
        Long eventId = 1L;
        Long organizerId = 10L;
        UserVO organizer = createUserVO(organizerId, Role.ROLE_USER);
        Event existingEvent = createUpcomingEvent();
        existingEvent.setOrganizerId(organizerId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        when(imageStorageService.deleteImage(anyString())).thenReturn(true);

        // When
        eventService.deleteEvent(eventId, organizer);

        // Then
        verify(eventRepository, times(1)).deleteById(eventId);
        verify(imageStorageService, atLeastOnce()).deleteImage(anyString());
    }

    @Test
    void deleteEvent_Failure_NotFound() {
        // Given
        Long eventId = 99L;
        Long userId = 10L;
        UserVO user = createUserVO(userId, Role.ROLE_USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When/Then
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> eventService.deleteEvent(eventId, user));

        assertEquals(ErrorMessage.EVENT_NOT_FOUND_BY_ID + eventId, thrown.getMessage());
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void removeAttender_Failure_NotAttending() {
        // Given
        Long eventId = 1L;
        UserVO user = createUserVO(20L, Role.ROLE_USER);
        Event event = createUpcomingEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttenderRepo.existsByEventIdAndUserId(eventId, user.getId())).thenReturn(false); // Not attending

        // When
        boolean result = eventService.removeAttender(eventId, user);

        // Then
        assertFalse(result);
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void removeAttender_Failure_PastEvent() {
        // Given
        Long eventId = 2L;
        UserVO user = createUserVO(20L, Role.ROLE_USER);
        Event event = createPassedEvent();
        EventDto eventDto = createMockEventDto(eventId);
        eventDto.setStatus(EventStatus.PASSED); // Event is PASSED

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(mapper.map(event, EventDto.class)).thenReturn(eventDto); // Mock toEventDto

        // When/Then
        BadRequestException thrown =
            assertThrows(BadRequestException.class, () -> eventService.removeAttender(eventId, user));

        assertEquals(ErrorMessage.EVENT_CANT_UNATTEND_PAST, thrown.getMessage());
        verify(eventAttenderRepo, never()).deleteByEventIdAndUserId(anyLong(), anyLong());
    }

}
