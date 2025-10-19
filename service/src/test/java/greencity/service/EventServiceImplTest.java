package greencity.service;

import greencity.dto.event.EventDto;
import greencity.entity.Event;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import greencity.enums.EventStatus;
import greencity.exception.exceptions.NotFoundException;
import greencity.repository.EventDateTimeLocationRepo;
import greencity.repository.EventImageRepo;
import greencity.repository.EventRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    private ImageStorageService imageStorageService;

    @Mock
    private ModelMapper mapper;

    @Mock
    private Clock clock;

    @InjectMocks
    private EventServiceImpl eventService;

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
}
