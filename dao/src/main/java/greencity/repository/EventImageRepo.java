package greencity.repository;

import greencity.entity.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EventImageRepo extends JpaRepository<EventImage, Long>,
    JpaSpecificationExecutor<EventImage> {
}
