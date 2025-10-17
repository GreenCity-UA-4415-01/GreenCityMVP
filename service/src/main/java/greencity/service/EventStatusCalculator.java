package greencity.service;

import greencity.entity.EventDateTimeLocation;
import greencity.enums.EventStatus;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class EventStatusCalculator {
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class EventStatusResult {
        private EventStatus status;
        private OffsetDateTime nearestStart;
        private OffsetDateTime nearestFinish;
    }

    public static EventStatusResult computeStatus(List<EventDateTimeLocation> dateLocations, OffsetDateTime now) {
        if (dateLocations == null || dateLocations.isEmpty()) {
            return EventStatusResult.builder()
                    .status(EventStatus.PASSED)
                    .nearestStart(null)
                    .nearestFinish(null)
                    .build();
        }

        // Check for LIVE status: is now between start and finish of any occurrence?
        Optional<EventDateTimeLocation> liveOccurrence = dateLocations.stream()
                .filter(loc -> !now.isBefore(loc.getStartDate()) && !now.isAfter(loc.getFinishDate()))
                .findFirst();

        if (liveOccurrence.isPresent()) {
            EventDateTimeLocation live = liveOccurrence.get();
            return EventStatusResult.builder()
                    .status(EventStatus.LIVE)
                    .nearestStart(live.getStartDate())
                    .nearestFinish(live.getFinishDate())
                    .build();
        }

        // Check for UPCOMING status: find earliest future occurrence
        Optional<EventDateTimeLocation> nextOccurrence = dateLocations.stream()
                .filter(loc -> now.isBefore(loc.getStartDate()))
                .min((a, b) -> a.getStartDate().compareTo(b.getStartDate()));

        if (nextOccurrence.isPresent()) {
            EventDateTimeLocation next = nextOccurrence.get();
            return EventStatusResult.builder()
                    .status(EventStatus.UPCOMING)
                    .nearestStart(next.getStartDate())
                    .nearestFinish(next.getFinishDate())
                    .build();
        }

        // All occurrences are in the past - PASSED status
        return EventStatusResult.builder()
                .status(EventStatus.PASSED)
                .nearestStart(null)
                .nearestFinish(null)
                .build();
    }
}
