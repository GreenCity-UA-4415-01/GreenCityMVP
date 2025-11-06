package greencity.service;

import greencity.entity.Notification;
import greencity.entity.User;
import greencity.enums.NotificationActionType;
import greencity.enums.NotificationObjectType;
import greencity.message.NotificationEventMessage;
import greencity.repository.NotificationRepo;
import greencity.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static greencity.constant.NotificationRoutingKeys.NOTIFICATIONS_WRITE_QUEUE;

/**
 * RabbitMQ consumer service for notification events.
 * Handles message consumption, validation, idempotency checking, username resolution,
 * and persistence of notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumerService {

    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;

    /**
     * Consumes notification events from RabbitMQ queue.
     * Processes each event by:
     * 1. Validating the message
     * 2. Checking for duplicates (idempotency)
     * 3. Resolving usernames if needed
     * 4. Creating and persisting notifications for each recipient
     *
     * @param event the notification event message
     */
    @RabbitListener(queues = NOTIFICATIONS_WRITE_QUEUE)
    @Transactional
    public void consumeNotificationEvent(NotificationEventMessage event) {
        log.info("Received notification event: idempotencyKey={}, actionType={}, objectType={}",
                event.getIdempotencyKey(), event.getActionType(), event.getObjectType());

        try {
            // Validate the event
            validateEvent(event);

            // Resolve usernames if not provided
            String actorUsernames = resolveActorUsernames(event);

            // Process notification for each recipient
            int processedCount = 0;
            int skippedCount = 0;

            for (Long recipientId : event.getRecipientUserIds()) {
                try {
                    if (processNotificationForRecipient(event, recipientId, actorUsernames)) {
                        processedCount++;
                    } else {
                        skippedCount++;
                        log.debug("Skipped duplicate notification for recipientId={}, idempotencyKey={}",
                                recipientId, event.getIdempotencyKey());
                    }
                } catch (Exception e) {
                    log.error("Error processing notification for recipientId={}, idempotencyKey={}: {}",
                            recipientId, event.getIdempotencyKey(), e.getMessage(), e);
                    // Continue processing other recipients even if one fails
                }
            }

            log.info("Processed notification event: processed={}, skipped={}, idempotencyKey={}",
                    processedCount, skippedCount, event.getIdempotencyKey());

        } catch (IllegalArgumentException e) {
            log.error("Invalid notification event: idempotencyKey={}, error={}",
                    event.getIdempotencyKey(), e.getMessage(), e);
            // Reject and send to DLQ - don't requeue invalid messages
            throw new AmqpRejectAndDontRequeueException("Invalid notification event: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error processing notification event: idempotencyKey={}, error={}",
                    event.getIdempotencyKey(), e.getMessage(), e);
            // Reject and send to DLQ - don't requeue on unexpected errors
            throw new AmqpRejectAndDontRequeueException("Error processing notification event: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the notification event message.
     *
     * @param event the event to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateEvent(NotificationEventMessage event) {
        if (event.getRecipientUserIds() == null || event.getRecipientUserIds().isEmpty()) {
            throw new IllegalArgumentException("Recipient user IDs cannot be null or empty");
        }

        if (event.getActorUserIds() == null || event.getActorUserIds().isEmpty()) {
            throw new IllegalArgumentException("Actor user IDs cannot be null or empty");
        }

        if (!StringUtils.hasText(event.getActionType())) {
            throw new IllegalArgumentException("Action type cannot be null or empty");
        }

        if (!StringUtils.hasText(event.getObjectType())) {
            throw new IllegalArgumentException("Object type cannot be null or empty");
        }

        if (!StringUtils.hasText(event.getIdempotencyKey())) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }

        // Validate action type enum
        if (NotificationActionType.fromValue(event.getActionType()) == null) {
            throw new IllegalArgumentException("Invalid action type: " + event.getActionType());
        }

        // Validate object type enum
        if (NotificationObjectType.fromValue(event.getObjectType()) == null) {
            throw new IllegalArgumentException("Invalid object type: " + event.getObjectType());
        }
    }

    /**
     * Resolves actor usernames from user IDs if not provided in the event.
     * Uses caching-friendly batch lookup.
     *
     * @param event the notification event
     * @return formatted actor usernames string
     */
    private String resolveActorUsernames(NotificationEventMessage event) {
        List<String> usernames;

        // If usernames are already provided, use them
        if (event.getActorUsernames() != null && !event.getActorUsernames().isEmpty()) {
            usernames = event.getActorUsernames();
        } else {
            // Otherwise, resolve from user IDs
            List<User> actors = userRepo.findAllById(event.getActorUserIds());

            if (actors.size() != event.getActorUserIds().size()) {
                log.warn("Some actor users not found. Expected={}, Found={}, ActorIds={}",
                        event.getActorUserIds().size(), actors.size(), event.getActorUserIds());
            }

            // Create a map for quick lookup
            Map<Long, String> userIdToNameMap = actors.stream()
                    .collect(Collectors.toMap(User::getId, User::getName));

            // Build username list in the same order as actorUserIds
            usernames = event.getActorUserIds().stream()
                    .map(userId -> userIdToNameMap.getOrDefault(userId, "Unknown User"))
                    .collect(Collectors.toList());
        }

        // Format usernames for display (e.g., "John Doe", "Jane Smith, and 2 others")
        return formatActorUsernames(usernames);
    }

    /**
     * Formats actor usernames for display.
     * If multiple actors, formats as "User1, User2, and N others" (max 2 shown).
     *
     * @param usernames list of usernames
     * @return formatted username string
     */
    private String formatActorUsernames(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return "Unknown User";
        }

        if (usernames.size() == 1) {
            return usernames.get(0);
        }

        if (usernames.size() == 2) {
            return usernames.get(0) + " and " + usernames.get(1);
        }

        // For 3 or more, show first 2 and count
        int othersCount = usernames.size() - 2;
        return usernames.get(0) + ", " + usernames.get(1) + ", and " + othersCount + " others";
    }

    /**
     * Processes a notification for a single recipient.
     * Checks for idempotency and creates the notification if it doesn't exist.
     *
     * @param event          the notification event
     * @param recipientId    the recipient user ID
     * @param actorUsernames the resolved actor usernames string
     * @return true if notification was created, false if duplicate (idempotency)
     */
    private boolean processNotificationForRecipient(
            NotificationEventMessage event,
            Long recipientId,
            String actorUsernames) {

        // Check for duplicate notification (idempotency)
        // Note: We check by recipient + action + object type + object ID
        // The idempotency key in the event is for reference, but we use these fields for actual deduplication
        if (event.getObjectId() != null && notificationRepo.existsByRecipientAndActionAndObject(
                recipientId,
                event.getActionType(),
                event.getObjectType(),
                event.getObjectId())) {
            log.debug("Duplicate notification detected and skipped: recipientId={}, actionType={}, objectType={}, objectId={}",
                    recipientId, event.getActionType(), event.getObjectType(), event.getObjectId());
            return false;
        }

        // Verify recipient exists
        User recipient = userRepo.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found: " + recipientId));

        // Truncate actor usernames if too long
        String actorUsernamesString = actorUsernames;
        if (actorUsernamesString != null && actorUsernamesString.length() > 255) {
            actorUsernamesString = actorUsernamesString.substring(0, 252) + "...";
        }

        // Create notification
        OffsetDateTime occurredAt = event.getOccurredAt() != null
                ? event.getOccurredAt()
                : OffsetDateTime.now();

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actorUsernames(actorUsernamesString)
                .actionType(event.getActionType())
                .objectType(event.getObjectType())
                .objectId(event.getObjectId())
                .objectTitle(event.getObjectTitle())
                .occurredAt(occurredAt)
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();

        notificationRepo.save(notification);

        log.debug("Created notification: id={}, recipientId={}, actionType={}, objectType={}",
                notification.getId(), recipientId, event.getActionType(), event.getObjectType());

        return true;
    }
}

