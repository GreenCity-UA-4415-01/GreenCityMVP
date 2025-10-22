package greencity.repository;

import greencity.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EventRepo extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    /**
     * Method that finds {@link Event} by id.
     *
     * @param id {@link Long}.
     * @return {@link Optional} of {@link Event}
     */
    Optional<Event> findById(Long id);

    /**
     * Find events created by a specific organizer, sorted by nearest start date.
     * Includes all events (past, present, future) to show complete history.
     *
     * @param organizerId the ID of the event organizer
     * @param pageable    pagination parameters
     * @return page of events created by the organizer
     */
    @Query(
        value = """
            SELECT e FROM Event e
            WHERE e.organizerId = :organizerId
            ORDER BY (SELECT MIN(l.startDate) FROM e.dateTimeLocations l) ASC
            """,
        countQuery = """
               SELECT COUNT(e) FROM Event e
               WHERE e.organizerId = :organizerId
            """)
    Page<Event> findByOrganizerIdOrderByNearestStart(@Param("organizerId") Long organizerId, Pageable pageable);
}