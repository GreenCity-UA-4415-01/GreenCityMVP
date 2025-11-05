package greencity.service;

import greencity.dto.PageableAdvancedDto;
import greencity.dto.notification.NotificationDto;
import greencity.entity.Notification;
import greencity.mapping.NotificationDtoMapper;
import greencity.repository.NotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link NotificationService}.
 * Provides methods for retrieving notifications with pagination and ordering.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepo notificationRepo;
    private final NotificationDtoMapper notificationDtoMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageableAdvancedDto<NotificationDto> findAllByUserId(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepo.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationDto> notificationDtos = notificationPage.getContent().stream()
                .map(notification -> notificationDtoMapper.convert(notification))
                .collect(Collectors.toList());

        return new PageableAdvancedDto<>(
                notificationDtos,
                notificationPage.getTotalElements(),
                notificationPage.getPageable().getPageNumber(),
                notificationPage.getTotalPages(),
                notificationPage.getNumber(),
                notificationPage.hasPrevious(),
                notificationPage.hasNext(),
                notificationPage.isFirst(),
                notificationPage.isLast()
        );
    }
}

