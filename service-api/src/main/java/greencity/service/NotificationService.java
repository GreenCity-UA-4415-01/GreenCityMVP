package greencity.service;

import greencity.dto.PageableAdvancedDto;
import greencity.dto.notification.NotificationDto;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    /**
     * Retrieves all notifications for the authenticated user, ordered by creation date
     * (newest first), with pagination support.
     *
     * @param userId  the ID of the user requesting notifications
     * @param pageable pagination parameters (page, size, sort)
     * @return page of notifications ordered by newest first
     */
    PageableAdvancedDto<NotificationDto> findAllByUserId(Long userId, Pageable pageable);
}