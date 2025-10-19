package greencity.repository;

import greencity.dto.event.EventDto;
import greencity.entity.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventImageRepo extends JpaRepository<EventImage, Long>,
    JpaSpecificationExecutor<EventImage> {
    /**
     * Method to delete all images related to an event ID.
     *
     * @param eventId {@link EventDto} id whose children {@link EventImage}
     *                instances will be deleted.
     */
    @Modifying
    @Query("DELETE FROM EventImage e WHERE e.event.id = :eventId")
    void deleteAllByEventId(@Param("eventId") Long eventId);
}
