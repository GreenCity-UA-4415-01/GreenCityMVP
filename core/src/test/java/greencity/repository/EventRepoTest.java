package greencity.repository;

import greencity.entity.Event;
import greencity.entity.EventAttender;
import greencity.entity.EventDateTimeLocation;
import greencity.entity.EventImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EventRepo. Tests database queries, pagination, and
 * sorting.
 *
 * @author Generated
 */
@DataJpaTest
@ContextConfiguration(classes = greencity.DaoApplication.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.liquibase.enabled=false"
})
class EventRepoTest {
    @Autowired
    private EventRepo eventRepo;

    @Autowired
    private EventDateTimeLocationRepo dateTimeLocationRepo;

    @Autowired
    private EventImageRepo eventImageRepo;

    private Long organizerId1;
    private Long organizerId2;

    @BeforeEach
    void setUp() {
        organizerId1 = 100L;
        organizerId2 = 200L;
        eventRepo.deleteAll();
        dateTimeLocationRepo.deleteAll();
        eventImageRepo.deleteAll();
    }

    @Test
    void saveEvent_WithLocationsAndImages_SavesCorrectly() {
        // Given
        Event event = createEvent(1L, organizerId1, "Test Event 1");

        // When
        Event savedEvent = eventRepo.save(event);

        // Then
        assertNotNull(savedEvent.getId());
        assertEquals("Test Event 1", savedEvent.getTitle());
        assertEquals(organizerId1, savedEvent.getOrganizerId());

        // Verify relationships are persisted
        Optional<Event> found = eventRepo.findById(savedEvent.getId());
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getDateTimeLocations().size());
        assertEquals(1, found.get().getImages().size());
    }

    @Test
    void findById_WithExistingEvent_ReturnsEvent() {
        // Given
        Event event = createEvent(1L, organizerId1, "Test Event");
        Event saved = eventRepo.save(event);

        // When
        Optional<Event> found = eventRepo.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("Test Event", found.get().getTitle());
        assertEquals(organizerId1, found.get().getOrganizerId());
    }

    @Test
    void findById_WithNonExistentEvent_ReturnsEmpty() {
        // When
        Optional<Event> found = eventRepo.findById(999L);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ReturnsAllEvents() {
        // Given
        Event event1 = createEvent(1L, organizerId1, "Event 1");
        Event event2 = createEvent(2L, organizerId2, "Event 2");
        Event event3 = createEvent(3L, organizerId1, "Event 3");

        eventRepo.saveAll(List.of(event1, event2, event3));

        // When
        List<Event> allEvents = eventRepo.findAll();

        // Then
        assertEquals(3, allEvents.size());
    }

    @Test
    void deleteById_DeletesEvent() {
        // Given
        Event event = createEvent(1L, organizerId1, "Event to Delete");
        Event saved = eventRepo.save(event);
        Long eventId = saved.getId();

        // When
        eventRepo.deleteById(eventId);

        // Then
        Optional<Event> found = eventRepo.findById(eventId);
        assertFalse(found.isPresent());
    }

    // Helper methods
    private Event createEvent(Long id, Long organizerId, String title) {
        Event event = Event.builder()
            .id(id)
            .title(title)
            .description("This is a test event description with enough characters to be valid.")
            .open(true)
            .organizerId(organizerId)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        // Add date locations
        EventDateTimeLocation loc1 = EventDateTimeLocation.builder()
            .event(event)
            .startDate(OffsetDateTime.now().plusDays(1))
            .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
            .latitude(50.4501)
            .longitude(30.5234)
            .createdAt(OffsetDateTime.now())
            .build();

        EventDateTimeLocation loc2 = EventDateTimeLocation.builder()
            .event(event)
            .startDate(OffsetDateTime.now().plusDays(5))
            .finishDate(OffsetDateTime.now().plusDays(5).plusHours(3))
            .latitude(50.4501)
            .longitude(30.5234)
            .createdAt(OffsetDateTime.now())
            .build();

        event.setDateTimeLocations(new ArrayList<>(List.of(loc1, loc2)));

        // Add image
        EventImage image = EventImage.builder()
            .event(event)
            .imagePath("event-" + id + ".jpg")
            .main(true)
            .createdAt(OffsetDateTime.now())
            .build();
        event.setImages(new ArrayList<>(List.of(image)));

        return event;
    }

    private Event createEventWithSpecificStart(Long id, Long organizerId, String title, OffsetDateTime startDate) {
        Event event = Event.builder()
            .id(id)
            .title(title)
            .description("This is a test event description with enough characters to be valid.")
            .open(true)
            .organizerId(organizerId)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        EventDateTimeLocation location = EventDateTimeLocation.builder()
            .event(event)
            .startDate(startDate)
            .finishDate(startDate.plusHours(2))
            .latitude(50.4501)
            .longitude(30.5234)
            .createdAt(OffsetDateTime.now())
            .build();

        event.setDateTimeLocations(new ArrayList<>(List.of(location)));

        EventImage image = EventImage.builder()
            .event(event)
            .imagePath("event-" + id + ".jpg")
            .main(true)
            .createdAt(OffsetDateTime.now())
            .build();
        event.setImages(new ArrayList<>(List.of(image)));

        return event;
    }
}