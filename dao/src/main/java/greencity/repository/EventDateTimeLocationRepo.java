package greencity.repository;

import greencity.entity.EventDateTimeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EventDateTimeLocationRepo extends JpaRepository<EventDateTimeLocation, Long>,
        JpaSpecificationExecutor<EventDateTimeLocation> {
    @Query("SELECT dtl FROM EventDateTimeLocation dtl WHERE dtl.event.id = :eventId ORDER BY dtl.startDate ASC")
    List<EventDateTimeLocation> findByEventIdOrderByStartDate(@Param("eventId") Long eventId);

    @Query("SELECT CASE WHEN COUNT(dtl) > 0 THEN true ELSE false END " +
            "FROM EventDateTimeLocation dtl " +
            "WHERE dtl.event.id = :eventId " +
            "AND :now >= dtl.startDate AND :now <= dtl.finishDate")
    boolean hasLiveOccurrence(@Param("eventId") Long eventId, @Param("now") OffsetDateTime now);

    @Query("SELECT MIN(dtl.startDate) FROM EventDateTimeLocation dtl " +
            "WHERE dtl.event.id = :eventId AND dtl.startDate > :now")
    OffsetDateTime findEarliestFutureStartDate(@Param("eventId") Long eventId, @Param("now") OffsetDateTime now);

    @Query("SELECT MAX(dtl.finishDate) FROM EventDateTimeLocation dtl WHERE dtl.event.id = :eventId")
    OffsetDateTime findLatestFinishDate(@Param("eventId") Long eventId);

    @Query("SELECT dtl FROM EventDateTimeLocation dtl " +
            "WHERE dtl.event.id = :eventId AND dtl.startDate > :now " +
            "ORDER BY dtl.startDate ASC")
    List<EventDateTimeLocation> findEarliestFutureOccurrence(@Param("eventId") Long eventId, @Param("now") OffsetDateTime now);

    @Query("SELECT dtl FROM EventDateTimeLocation dtl " +
            "WHERE dtl.event.id = :eventId " +
            "AND :now >= dtl.startDate AND :now <= dtl.finishDate " +
            "ORDER BY dtl.startDate ASC")
    List<EventDateTimeLocation> findLiveOccurrence(@Param("eventId") Long eventId, @Param("now") OffsetDateTime now);
}

