package greencity.service;

import greencity.dto.PageableAdvancedDto;
import greencity.dto.notification.NotificationDto;
import greencity.entity.Notification;
import greencity.entity.User;
import greencity.exception.exceptions.NotFoundException;
import greencity.mapping.NotificationDtoMapper;
import greencity.repository.NotificationRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static greencity.constant.ErrorMessage.NOTIFICATION_NOT_FOUND_BY_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock
    private NotificationRepo notificationRepo;

    @Mock
    private NotificationDtoMapper notificationDtoMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private List<Notification> testNotifications;
    private List<NotificationDto> testNotificationDtos;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        testNotifications = createTestNotifications();
        testNotificationDtos = createTestNotificationDtos();
    }

    @Test
    void findAllByUserId_Success_ReturnsPageableDto() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(
                testNotifications,
                pageable,
                testNotifications.size()
        );

        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);
        when(notificationDtoMapper.convert(testNotifications.get(0)))
                .thenReturn(testNotificationDtos.get(0));
        when(notificationDtoMapper.convert(testNotifications.get(1)))
                .thenReturn(testNotificationDtos.get(1));

        // When
        PageableAdvancedDto<NotificationDto> result = notificationService.findAllByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getPage().size());
        assertEquals(2L, result.getTotalElements());
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertFalse(result.isHasPrevious());
        assertFalse(result.isHasNext());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());

        assertEquals(testNotificationDtos.get(0).getId(), result.getPage().get(0).getId());
        assertEquals(testNotificationDtos.get(0).getActorUsernames(), result.getPage().get(0).getActorUsernames());
        assertEquals(testNotificationDtos.get(0).getAction(), result.getPage().get(0).getAction());
        assertEquals(testNotificationDtos.get(0).getIsRead(), result.getPage().get(0).getIsRead());

        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
        verify(notificationDtoMapper, times(2)).convert(any(Notification.class));
    }

    @Test
    void findAllByUserId_EmptyResult_ReturnsEmptyPageableDto() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> emptyPage = new PageImpl<>(
                new ArrayList<>(),
                pageable,
                0
        );

        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(emptyPage);

        // When
        PageableAdvancedDto<NotificationDto> result = notificationService.findAllByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getPage().isEmpty());
        assertEquals(0L, result.getTotalElements());
        assertEquals(0, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertFalse(result.isHasPrevious());
        assertFalse(result.isHasNext());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());

        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
        verify(notificationDtoMapper, never()).convert(any(Notification.class));
    }

    @Test
    void findAllByUserId_WithPagination_ReturnsCorrectPage() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(1, 5);
        List<Notification> secondPageNotifications = List.of(testNotifications.get(0));
        Page<Notification> notificationPage = new PageImpl<>(
                secondPageNotifications,
                pageable,
                12L  // Total elements
        );

        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);
        when(notificationDtoMapper.convert(testNotifications.get(0)))
                .thenReturn(testNotificationDtos.get(0));

        // When
        PageableAdvancedDto<NotificationDto> result = notificationService.findAllByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPage().size());
        assertEquals(12L, result.getTotalElements());
        assertEquals(1, result.getCurrentPage());
        assertEquals(3, result.getTotalPages()); // 12 elements / 5 per page = 3 pages
        assertEquals(1, result.getNumber());
        assertTrue(result.isHasPrevious());
        assertTrue(result.isHasNext());
        assertFalse(result.isFirst());
        assertFalse(result.isLast());

        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
        verify(notificationDtoMapper, times(1)).convert(any(Notification.class));
    }

    @Test
    void findAllByUserId_LargeDataset_HandlesCorrectly() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        List<Notification> largeNotificationList = createLargeNotificationList(25);
        Page<Notification> notificationPage = new PageImpl<>(
                largeNotificationList.subList(0, 20),
                pageable,
                25L
        );

        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);
        when(notificationDtoMapper.convert(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    return NotificationDto.builder()
                            .id(notification.getId())
                            .actorUsernames(notification.getActorUsernames())
                            .action(notification.getActionType())
                            .objectTitle(notification.getObjectTitle())
                            .occurredAt(notification.getOccurredAt())
                            .isRead(notification.getIsRead())
                            .build();
                });

        // When
        PageableAdvancedDto<NotificationDto> result = notificationService.findAllByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(20, result.getPage().size());
        assertEquals(25L, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertTrue(result.isHasNext());
        assertFalse(result.isHasNext() && result.isLast());

        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
        verify(notificationDtoMapper, times(20)).convert(any(Notification.class));
    }

    @Test
    void findAllByUserId_RepositoryThrowsException_PropagatesException() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.findAllByUserId(userId, pageable);
        });

        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
        verify(notificationDtoMapper, never()).convert(any(Notification.class));
    }

    @Test
    void findAllByUserId_DifferentUserIds_ScopesCorrectly() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> user1Page = new PageImpl<>(
                List.of(testNotifications.get(0)),
                pageable,
                1L
        );

        Page<Notification> user2Page = new PageImpl<>(
                List.of(testNotifications.get(1)),
                pageable,
                1L
        );

        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId1), eq(pageable)))
                .thenReturn(user1Page);
        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId2), eq(pageable)))
                .thenReturn(user2Page);
        when(notificationDtoMapper.convert(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    return NotificationDto.builder()
                            .id(notification.getId())
                            .actorUsernames(notification.getActorUsernames())
                            .action(notification.getActionType())
                            .objectTitle(notification.getObjectTitle())
                            .occurredAt(notification.getOccurredAt())
                            .isRead(notification.getIsRead())
                            .build();
                });

        // When
        PageableAdvancedDto<NotificationDto> result1 = notificationService.findAllByUserId(userId1, pageable);
        PageableAdvancedDto<NotificationDto> result2 = notificationService.findAllByUserId(userId2, pageable);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(1, result1.getPage().size());
        assertEquals(1, result2.getPage().size());
        assertEquals(1L, result1.getTotalElements());
        assertEquals(1L, result2.getTotalElements());

        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId1), eq(pageable));
        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId2), eq(pageable));
    }

    @Test
    void findAllByUserId_OrderedByCreatedAtDesc_VerifiesOrdering() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> orderedNotifications = List.of(
                createNotification(1L, OffsetDateTime.now().minusHours(1)),
                createNotification(2L, OffsetDateTime.now().minusHours(2)),
                createNotification(3L, OffsetDateTime.now().minusHours(3))
        );
        Page<Notification> notificationPage = new PageImpl<>(
                orderedNotifications,
                pageable,
                3L
        );

        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);
        when(notificationDtoMapper.convert(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    return NotificationDto.builder()
                            .id(notification.getId())
                            .actorUsernames(notification.getActorUsernames())
                            .action(notification.getActionType())
                            .objectTitle(notification.getObjectTitle())
                            .occurredAt(notification.getOccurredAt())
                            .isRead(notification.getIsRead())
                            .build();
                });

        // When
        PageableAdvancedDto<NotificationDto> result = notificationService.findAllByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getPage().size());
        // Verify that notifications are ordered from newest to oldest
        assertEquals(1L, result.getPage().get(0).getId());
        assertEquals(2L, result.getPage().get(1).getId());
        assertEquals(3L, result.getPage().get(2).getId());

        verify(notificationRepo, times(1)).findByRecipientIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
    }

    private List<Notification> createTestNotifications() {
        Notification notification1 = Notification.builder()
                .id(1L)
                .recipient(testUser)
                .actorUsernames("john.doe")
                .actionType("liked")
                .objectType("news")
                .objectId("123")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(false)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();

        Notification notification2 = Notification.builder()
                .id(2L)
                .recipient(testUser)
                .actorUsernames("jane.smith")
                .actionType("commented")
                .objectType("news")
                .objectId("456")
                .objectTitle("Another News")
                .occurredAt(OffsetDateTime.now().minusDays(1))
                .isRead(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        return List.of(notification1, notification2);
    }

    private List<NotificationDto> createTestNotificationDtos() {
        NotificationDto dto1 = NotificationDto.builder()
                .id(1L)
                .actorUsernames("john.doe")
                .action("liked")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(false)
                .build();

        NotificationDto dto2 = NotificationDto.builder()
                .id(2L)
                .actorUsernames("jane.smith")
                .action("commented")
                .objectTitle("Another News")
                .occurredAt(OffsetDateTime.now().minusDays(1))
                .isRead(true)
                .build();

        return List.of(dto1, dto2);
    }

    private List<Notification> createLargeNotificationList(int count) {
        List<Notification> notifications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            notifications.add(createNotification(
                    (long) i + 1,
                    OffsetDateTime.now().minusHours(count - i)
            ));
        }
        return notifications;
    }

    private Notification createNotification(Long id, OffsetDateTime createdAt) {
        return Notification.builder()
                .id(id)
                .recipient(testUser)
                .actorUsernames("user" + id)
                .actionType("liked")
                .objectType("news")
                .objectId("id" + id)
                .objectTitle("News " + id)
                .occurredAt(createdAt)
                .isRead(false)
                .createdAt(createdAt)
                .build();
    }

    @Test
    void markAsRead_Success_MarksNotificationAsRead() {
        // Given
        Long notificationId = 1L;
        Long userId = 1L;
        Notification notification = Notification.builder()
                .id(notificationId)
                .recipient(testUser)
                .actorUsernames("john.doe")
                .actionType("liked")
                .objectType("news")
                .objectId("123")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(false)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();

        Notification savedNotification = Notification.builder()
                .id(notificationId)
                .recipient(testUser)
                .actorUsernames("john.doe")
                .actionType("liked")
                .objectType("news")
                .objectId("123")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(true)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();

        NotificationDto expectedDto = NotificationDto.builder()
                .id(notificationId)
                .actorUsernames("john.doe")
                .action("liked")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(true)
                .build();

        when(notificationRepo.findByIdAndRecipientId(notificationId, userId))
                .thenReturn(notification);
        when(notificationRepo.save(any(Notification.class)))
                .thenReturn(savedNotification);
        when(notificationDtoMapper.convert(savedNotification))
                .thenReturn(expectedDto);

        // When
        NotificationDto result = notificationService.markAsRead(notificationId, userId);

        // Then
        assertNotNull(result);
        assertEquals(notificationId, result.getId());
        assertTrue(result.getIsRead());
        assertEquals("john.doe", result.getActorUsernames());
        assertEquals("liked", result.getAction());

        verify(notificationRepo, times(1)).findByIdAndRecipientId(notificationId, userId);
        verify(notificationRepo, times(1)).save(any(Notification.class));
        verify(notificationDtoMapper, times(1)).convert(savedNotification);
    }

    @Test
    void markAsRead_NotificationNotFound_ThrowsNotFoundException() {
        // Given
        Long notificationId = 999L;
        Long userId = 1L;

        when(notificationRepo.findByIdAndRecipientId(notificationId, userId))
                .thenReturn(null);

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            notificationService.markAsRead(notificationId, userId);
        });

        assertEquals(NOTIFICATION_NOT_FOUND_BY_ID + notificationId, exception.getMessage());
        verify(notificationRepo, times(1)).findByIdAndRecipientId(notificationId, userId);
        verify(notificationRepo, never()).save(any(Notification.class));
        verify(notificationDtoMapper, never()).convert(any(Notification.class));
    }

    @Test
    void markAsRead_NotificationBelongsToDifferentUser_ThrowsNotFoundException() {
        // Given
        Long notificationId = 1L;
        Long userId = 2L; // Different user

        when(notificationRepo.findByIdAndRecipientId(notificationId, userId))
                .thenReturn(null);

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            notificationService.markAsRead(notificationId, userId);
        });

        assertEquals(NOTIFICATION_NOT_FOUND_BY_ID + notificationId, exception.getMessage());
        verify(notificationRepo, times(1)).findByIdAndRecipientId(notificationId, userId);
        verify(notificationRepo, never()).save(any(Notification.class));
        verify(notificationDtoMapper, never()).convert(any(Notification.class));
    }

    @Test
    void markAsRead_AlreadyRead_StillMarksAsRead() {
        // Given
        Long notificationId = 1L;
        Long userId = 1L;
        Notification notification = Notification.builder()
                .id(notificationId)
                .recipient(testUser)
                .actorUsernames("john.doe")
                .actionType("liked")
                .objectType("news")
                .objectId("123")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(true) // Already read
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();

        Notification savedNotification = Notification.builder()
                .id(notificationId)
                .recipient(testUser)
                .actorUsernames("john.doe")
                .actionType("liked")
                .objectType("news")
                .objectId("123")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(true)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .build();

        NotificationDto expectedDto = NotificationDto.builder()
                .id(notificationId)
                .actorUsernames("john.doe")
                .action("liked")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(true)
                .build();

        when(notificationRepo.findByIdAndRecipientId(notificationId, userId))
                .thenReturn(notification);
        when(notificationRepo.save(any(Notification.class)))
                .thenReturn(savedNotification);
        when(notificationDtoMapper.convert(savedNotification))
                .thenReturn(expectedDto);

        // When
        NotificationDto result = notificationService.markAsRead(notificationId, userId);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsRead());
        verify(notificationRepo, times(1)).findByIdAndRecipientId(notificationId, userId);
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_Success_ReturnsCount() {
        // Given
        Long userId = 1L;
        int expectedCount = 5;

        when(notificationRepo.markAllAsReadByRecipientId(userId))
                .thenReturn(expectedCount);

        // When
        int result = notificationService.markAllAsRead(userId);

        // Then
        assertEquals(expectedCount, result);
        verify(notificationRepo, times(1)).markAllAsReadByRecipientId(userId);
    }

    @Test
    void markAllAsRead_NoUnreadNotifications_ReturnsZero() {
        // Given
        Long userId = 1L;
        int expectedCount = 0;

        when(notificationRepo.markAllAsReadByRecipientId(userId))
                .thenReturn(expectedCount);

        // When
        int result = notificationService.markAllAsRead(userId);

        // Then
        assertEquals(0, result);
        verify(notificationRepo, times(1)).markAllAsReadByRecipientId(userId);
    }

    @Test
    void markAllAsRead_DifferentUsers_ScopesCorrectly() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        int count1 = 3;
        int count2 = 7;

        when(notificationRepo.markAllAsReadByRecipientId(userId1))
                .thenReturn(count1);
        when(notificationRepo.markAllAsReadByRecipientId(userId2))
                .thenReturn(count2);

        // When
        int result1 = notificationService.markAllAsRead(userId1);
        int result2 = notificationService.markAllAsRead(userId2);

        // Then
        assertEquals(count1, result1);
        assertEquals(count2, result2);
        verify(notificationRepo, times(1)).markAllAsReadByRecipientId(userId1);
        verify(notificationRepo, times(1)).markAllAsReadByRecipientId(userId2);
    }
}

