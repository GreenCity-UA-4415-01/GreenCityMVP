package greencity.repository;

import greencity.entity.EventDateTimeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDateTimeLocationRepo extends JpaRepository<EventDateTimeLocation, Long>,
    JpaSpecificationExecutor<EventDateTimeLocation> {
}
