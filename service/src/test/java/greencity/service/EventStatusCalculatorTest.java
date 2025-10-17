package greencity.service;

import greencity.entity.Event;
import greencity.entity.EventDateTimeLocation;
import greencity.enums.EventStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventStatusCalculator.
 */
class EventStatusCalculatorTest {

    @Test
    void computeStatus_WithLiveOccurrence_ReturnsLive() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = createDateLocations(
                now.minusHours(1), now.plusHours(1)
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.LIVE, result.getStatus());
        assertNotNull(result.getNearestStart());
        assertNotNull(result.getNearestFinish());
        assertEquals(dateLocations.get(0).getStartDate(), result.getNearestStart());
        assertEquals(dateLocations.get(0).getFinishDate(), result.getNearestFinish());
    }

    @Test
    void computeStatus_WithUpcomingOccurrence_ReturnsUpcoming() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = createDateLocations(
                now.plusDays(1), now.plusDays(1).plusHours(2)
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.UPCOMING, result.getStatus());
        assertNotNull(result.getNearestStart());
        assertNotNull(result.getNearestFinish());
        assertEquals(dateLocations.get(0).getStartDate(), result.getNearestStart());
        assertEquals(dateLocations.get(0).getFinishDate(), result.getNearestFinish());
    }

    @Test
    void computeStatus_WithPassedOccurrence_ReturnsPassed() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = createDateLocations(
                now.minusDays(2), now.minusDays(1)
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.PASSED, result.getStatus());
        assertNull(result.getNearestStart());
        assertNull(result.getNearestFinish());
    }

    @Test
    void computeStatus_WithMultipleOccurrences_FirstIsLive_ReturnsLive() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        Event event = Event.builder().id(1L).build();

        List<EventDateTimeLocation> dateLocations = Arrays.asList(
                createDateLocation(event, 1L, now.minusHours(1), now.plusHours(1)),
                createDateLocation(event, 2L, now.plusDays(1), now.plusDays(1).plusHours(2))
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.LIVE, result.getStatus());
        assertEquals(dateLocations.get(0).getStartDate(), result.getNearestStart());
        assertEquals(dateLocations.get(0).getFinishDate(), result.getNearestFinish());
    }

    @Test
    void computeStatus_WithMultipleOccurrences_SecondIsLive_ReturnsLive() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        Event event = Event.builder().id(1L).build();

        List<EventDateTimeLocation> dateLocations = Arrays.asList(
                createDateLocation(event, 1L, now.minusDays(1), now.minusDays(1).plusHours(2)),
                createDateLocation(event, 2L, now.minusHours(1), now.plusHours(1)),
                createDateLocation(event, 3L, now.plusDays(1), now.plusDays(1).plusHours(2))
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.LIVE, result.getStatus());
        assertEquals(dateLocations.get(1).getStartDate(), result.getNearestStart());
        assertEquals(dateLocations.get(1).getFinishDate(), result.getNearestFinish());
    }

    @Test
    void computeStatus_WithMultipleUpcomingOccurrences_ReturnsEarliest() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        Event event = Event.builder().id(1L).build();

        List<EventDateTimeLocation> dateLocations = Arrays.asList(
                createDateLocation(event, 1L, now.plusDays(3), now.plusDays(3).plusHours(2)),
                createDateLocation(event, 2L, now.plusDays(1), now.plusDays(1).plusHours(2)),
                createDateLocation(event, 3L, now.plusDays(5), now.plusDays(5).plusHours(2))
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.UPCOMING, result.getStatus());
        assertEquals(dateLocations.get(1).getStartDate(), result.getNearestStart());
        assertEquals(dateLocations.get(1).getFinishDate(), result.getNearestFinish());
    }

    @Test
    void computeStatus_WithMixedOccurrences_HasLive_ReturnsLive() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        Event event = Event.builder().id(1L).build();

        List<EventDateTimeLocation> dateLocations = Arrays.asList(
                createDateLocation(event, 1L, now.minusDays(2), now.minusDays(1)),  // PASSED
                createDateLocation(event, 2L, now.minusHours(1), now.plusHours(1)), // LIVE
                createDateLocation(event, 3L, now.plusDays(1), now.plusDays(1).plusHours(2)) // UPCOMING
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.LIVE, result.getStatus());
        assertEquals(dateLocations.get(1).getStartDate(), result.getNearestStart());
    }

    @Test
    void computeStatus_WithEmptyList_ReturnsPassed() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = Collections.emptyList();

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.PASSED, result.getStatus());
        assertNull(result.getNearestStart());
        assertNull(result.getNearestFinish());
    }

    @Test
    void computeStatus_WithNullList_ReturnsPassed() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(null, now);

        // Then
        assertEquals(EventStatus.PASSED, result.getStatus());
        assertNull(result.getNearestStart());
        assertNull(result.getNearestFinish());
    }

    @Test
    void computeStatus_AtExactStartTime_ReturnsLive() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = createDateLocations(
                now, now.plusHours(2)
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.LIVE, result.getStatus());
    }

    @Test
    void computeStatus_AtExactFinishTime_ReturnsLive() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = createDateLocations(
                now.minusHours(2), now
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.LIVE, result.getStatus());
    }

    @Test
    void computeStatus_OneSecondBeforeStart_ReturnsUpcoming() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = createDateLocations(
                now.plusSeconds(1), now.plusHours(2)
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.UPCOMING, result.getStatus());
    }

    @Test
    void computeStatus_OneSecondAfterFinish_ReturnsPassed() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        List<EventDateTimeLocation> dateLocations = createDateLocations(
                now.minusHours(2), now.minusSeconds(1)
        );

        // When
        EventStatusCalculator.EventStatusResult result =
                EventStatusCalculator.computeStatus(dateLocations, now);

        // Then
        assertEquals(EventStatus.PASSED, result.getStatus());
    }

    // Helper methods
    private List<EventDateTimeLocation> createDateLocations(OffsetDateTime start, OffsetDateTime finish) {
        Event event = Event.builder().id(1L).build();
        return Arrays.asList(
                createDateLocation(event, 1L, start, finish)
        );
    }

    private EventDateTimeLocation createDateLocation(Event event, Long id,
                                                     OffsetDateTime start, OffsetDateTime finish) {
        return EventDateTimeLocation.builder()
                .id(id)
                .event(event)
                .startDate(start)
                .finishDate(finish)
                .latitude(50.4501)
                .longitude(30.5234)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}


