package greencity.repository;

import greencity.entity.Event;
import greencity.entity.EventAttender;
import greencity.entity.EventAttenderId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;

@Repository
public interface EventAttenderRepo extends JpaRepository<EventAttender, EventAttenderId> {
    @Query("""
        SELECT e FROM Event e
        WHERE e.id IN (
            SELECT DISTINCT ea.eventId
            FROM EventAttender ea
            JOIN EventDateTimeLocation edtl ON edtl.event.id = ea.eventId
            WHERE ea.userId = :userId
            AND (:eventType = 'BOTH' OR
                 (:eventType = 'ONLINE' AND edtl.onlineLink IS NOT NULL) OR
                 (:eventType = 'PLACE' AND edtl.latitude IS NOT NULL AND edtl.longitude IS NOT NULL))
        )
        ORDER BY
            CASE WHEN :eventType = 'PLACE' AND :userLatitude IS NOT NULL AND :userLongitude IS NOT NULL
                 THEN (SELECT MIN(6371 * acos(cos(radians(CAST(:userLatitude AS double))) * cos(radians(loc.latitude)) *
                       cos(radians(loc.longitude) - radians(CAST(:userLongitude AS double))) +
                       sin(radians(CAST(:userLatitude AS double))) * sin(radians(loc.latitude))))
                       FROM EventDateTimeLocation loc
                       WHERE loc.event.id = e.id
                       AND loc.startDate >= :currentTime
                       AND loc.latitude IS NOT NULL AND loc.longitude IS NOT NULL)
                 ELSE e.id END ASC
        """)
    Page<Event> findJoinedEventsWithSorting(
        @Param("userId") Long userId,
        @Param("currentTime") OffsetDateTime currentTime,
        @Param("eventType") String eventType,
        @Param("userLatitude") Double userLatitude,
        @Param("userLongitude") Double userLongitude,
        Pageable pageable);

    @Query("""
        SELECT DISTINCT e FROM Event e
        JOIN e.dateTimeLocations edtl
        JOIN EventAttender ea ON ea.eventId = e.id
        WHERE ea.userId = :userId
        AND edtl.startDate >= :currentTime OR edtl.startDate < :currentTime
        ORDER BY e.id ASC
        """)
    Page<Event> findJoinedEventsDefaultSorting(
        @Param("userId") Long userId,
        @Param("currentTime") OffsetDateTime currentTime,
        Pageable pageable);

    /**
     * Check if a user is an attender of a specific event.
     *
     * @param eventId the ID of the event
     * @param userId  the ID of the user
     * @return true if the user is an attender, false otherwise
     */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    /**
     * Delete an attender from an event.
     *
     * @param eventId the ID of the event
     * @param userId  the ID of the user
     * @return number of deleted records
     */
    int deleteByEventIdAndUserId(Long eventId, Long userId);
}