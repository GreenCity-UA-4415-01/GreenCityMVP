package greencity.repository;

import greencity.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
