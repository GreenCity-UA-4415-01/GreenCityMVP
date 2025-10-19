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
        SELECT DISTINCT e FROM Event e
        JOIN e.dateTimeLocations edtl
        JOIN EventAttender ea ON ea.eventId = e.id
        WHERE ea.userId = :userId
        AND edtl.startDate >= :currentTime
        AND (:eventType = 'BOTH' OR
             (:eventType = 'ONLINE' AND edtl.onlineLink IS NOT NULL) OR
             (:eventType = 'PLACE' AND edtl.latitude IS NOT NULL AND edtl.longitude IS NOT NULL))
        ORDER BY
            CASE WHEN :eventType = 'PLACE' AND :userLatitude IS NOT NULL AND :userLongitude IS NOT NULL
                 THEN (6371 * acos(cos(radians(:userLatitude)) * cos(radians(edtl.latitude)) *
                       cos(radians(edtl.longitude) - radians(:userLongitude)) +
                       sin(radians(:userLatitude)) * sin(radians(edtl.latitude))))
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
        AND edtl.startDate >= :currentTime
        ORDER BY e.id ASC
        """)
    Page<Event> findJoinedEventsDefaultSorting(
        @Param("userId") Long userId,
        @Param("currentTime") OffsetDateTime currentTime,
        Pageable pageable);
}