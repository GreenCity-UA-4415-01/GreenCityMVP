package greencity.repository;

import greencity.dto.event.EventDto;
import greencity.entity.EventDateTimeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDateTimeLocationRepo extends JpaRepository<EventDateTimeLocation, Long>,
    JpaSpecificationExecutor<EventDateTimeLocation> {
    /**
     * New method to delete all date/locations related to an event ID.
     *
     * @param eventId {@link EventDto} id whose children {@link EventDateTimeLocation} instances will be deleted.
     */
    @Modifying
    @Query("DELETE FROM EventDateTimeLocation d WHERE d.event.id = :eventId")
    void deleteAllByEventId(@Param("eventId") Long eventId);
}
