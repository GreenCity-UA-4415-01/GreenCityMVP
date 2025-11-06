package greencity.service;

import greencity.dto.PageableAdvancedDto;
import greencity.dto.notification.NotificationDto;
import greencity.entity.Notification;
import greencity.exception.exceptions.NotFoundException;
import greencity.mapping.NotificationDtoMapper;
import greencity.repository.NotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import static greencity.constant.ErrorMessage.NOTIFICATION_NOT_FOUND_BY_ID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepo notificationRepo;
    private final NotificationDtoMapper notificationDtoMapper;

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

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepo.findByIdAndRecipientId(notificationId, userId);
        if (notification == null) {
            throw new NotFoundException(NOTIFICATION_NOT_FOUND_BY_ID + notificationId);
        }
        notification.setIsRead(true);
        Notification savedNotification = notificationRepo.save(notification);
        return notificationDtoMapper.convert(savedNotification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepo.markAllAsReadByRecipientId(userId);
    }
}

