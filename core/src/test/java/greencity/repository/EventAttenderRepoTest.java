package greencity.repository;

import greencity.entity.Event;
import greencity.entity.EventAttender;
import greencity.entity.EventDateTimeLocation;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EventAttenderRepo.
 * Tests custom queries, attendance management, and event filtering.
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
class EventAttenderRepoTest {
    @Autowired
    private EventAttenderRepo eventAttenderRepo;

    @Autowired
    private EventRepo eventRepo;

    @Autowired
    private EventDateTimeLocationRepo dateTimeLocationRepo;

    private Event placeEvent;
    private Event onlineEvent;
    private Event hybridEvent;
    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        userId1 = 100L;
        userId2 = 200L;

        eventAttenderRepo.deleteAll();
        dateTimeLocationRepo.deleteAll();
        eventRepo.deleteAll();

        // Create events
        placeEvent = createPlaceEvent(1L);
        onlineEvent = createOnlineEvent(2L);
        hybridEvent = createHybridEvent(3L);

        eventRepo.saveAll(List.of(placeEvent, onlineEvent, hybridEvent));
    }

    @Test
    void existsByEventIdAndUserId_WhenAttenderNotExists_ReturnsFalse() {
        // When
        boolean exists = eventAttenderRepo.existsByEventIdAndUserId(placeEvent.getId(), userId1);

        // Then
        assertFalse(exists);
    }

    @Test
    void deleteByEventIdAndUserId_WithNonExistentAttender_ReturnsZero() {
        // When
        int deletedCount = eventAttenderRepo.deleteByEventIdAndUserId(999L, 999L);

        // Then
        assertEquals(0, deletedCount);
    }

    @Test
    void findJoinedEventsDefaultSorting_WithNoJoinedEvents_ReturnsEmpty() {
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> result = eventAttenderRepo.findJoinedEventsDefaultSorting(
                999L, OffsetDateTime.now(), pageable
        );

        // Then
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void saveAndFind_EventAttenderPersistence() {
        // Given
        EventAttender attender = EventAttender.builder()
                .eventId(placeEvent.getId())
                .userId(userId1)
                .createdAt(OffsetDateTime.now())
                .build();

        // When
        EventAttender saved = eventAttenderRepo.save(attender);

        // Then
        assertNotNull(saved);
        assertEquals(placeEvent.getId(), saved.getEventId());
        assertEquals(userId1, saved.getUserId());
        assertNotNull(saved.getCreatedAt());
    }

    // Helper methods to create different event types
    private Event createPlaceEvent(Long id) {
        Event event = Event.builder()
                .id(id)
                .title("Place Event")
                .description("This is a test event description with enough characters.")
                .open(true)
                .organizerId(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        EventDateTimeLocation location = EventDateTimeLocation.builder()
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(50.4501)
                .longitude(30.5234)
                .onlineLink(null)
                .createdAt(OffsetDateTime.now())
                .build();

        event.setDateTimeLocations(new ArrayList<>(List.of(location)));
        return event;
    }

    private Event createOnlineEvent(Long id) {
        Event event = Event.builder()
                .id(id)
                .title("Online Event")
                .description("This is a test event description with enough characters.")
                .open(true)
                .organizerId(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        EventDateTimeLocation location = EventDateTimeLocation.builder()
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(null)
                .longitude(null)
                .onlineLink("https://example.com/meeting")
                .createdAt(OffsetDateTime.now())
                .build();

        event.setDateTimeLocations(new ArrayList<>(List.of(location)));
        return event;
    }

    private Event createHybridEvent(Long id) {
        Event event = Event.builder()
                .id(id)
                .title("Hybrid Event")
                .description("This is a test event description with enough characters.")
                .open(true)
                .organizerId(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        EventDateTimeLocation location1 = EventDateTimeLocation.builder()
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(1))
                .finishDate(OffsetDateTime.now().plusDays(1).plusHours(2))
                .latitude(50.4501)
                .longitude(30.5234)
                .onlineLink(null)
                .createdAt(OffsetDateTime.now())
                .build();

        EventDateTimeLocation location2 = EventDateTimeLocation.builder()
                .event(event)
                .startDate(OffsetDateTime.now().plusDays(2))
                .finishDate(OffsetDateTime.now().plusDays(2).plusHours(2))
                .latitude(null)
                .longitude(null)
                .onlineLink("https://example.com/meeting")
                .createdAt(OffsetDateTime.now())
                .build();

        event.setDateTimeLocations(new ArrayList<>(List.of(location1, location2)));
        return event;
    }
}



