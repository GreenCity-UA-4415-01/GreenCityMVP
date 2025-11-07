package greencity.util;

import greencity.enums.NotificationActionType;
import greencity.enums.NotificationObjectType;
import greencity.message.NotificationEventMessage;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Utility class for building NotificationEventMessage objects.
 * Provides convenient methods for creating notification events with proper
 * idempotency keys and routing keys.
 */
public final class NotificationEventBuilder {
    private NotificationEventBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Builds an idempotency key for a notification event.
     * Format: "{actionType}:{objectId}:{recipientUserId}"
     *
     * @param actionType    the action type
     * @param objectId      the object ID
     * @param recipientUserId the recipient user ID
     * @return the idempotency key
     */
    public static String buildIdempotencyKey(String actionType, String objectId, Long recipientUserId) {
        return String.format("%s:%s:%d", actionType, objectId, recipientUserId);
    }

    /**
     * Builds an idempotency key for a notification event using enums.
     *
     * @param actionType    the action type enum
     * @param objectId      the object ID
     * @param recipientUserId the recipient user ID
     * @return the idempotency key
     */
    public static String buildIdempotencyKey(NotificationActionType actionType, String objectId, Long recipientUserId) {
        return buildIdempotencyKey(actionType.getValue(), objectId, recipientUserId);
    }

    /**
     * Creates a NotificationEventMessage builder with common defaults.
     *
     * @param recipientUserIds list of recipient user IDs
     * @param actorUserIds     list of actor user IDs
     * @param actionType       the action type
     * @param objectType       the object type
     * @return a NotificationEventMessage builder
     */
    public static NotificationEventMessage.NotificationEventMessageBuilder builder(
            List<Long> recipientUserIds,
            List<Long> actorUserIds,
            NotificationActionType actionType,
            NotificationObjectType objectType) {
        return NotificationEventMessage.builder()
                .recipientUserIds(recipientUserIds)
                .actorUserIds(actorUserIds)
                .actionType(actionType.getValue())
                .objectType(objectType.getValue())
                .occurredAt(OffsetDateTime.now());
    }

    /**
     * Creates a NotificationEventMessage for a single recipient.
     *
     * @param recipientUserId the recipient user ID
     * @param actorUserIds    list of actor user IDs
     * @param actionType      the action type
     * @param objectType      the object type
     * @param objectId        the object ID
     * @param objectTitle     the object title
     * @return a NotificationEventMessage
     */
    public static NotificationEventMessage createForSingleRecipient(
            Long recipientUserId,
            List<Long> actorUserIds,
            NotificationActionType actionType,
            NotificationObjectType objectType,
            String objectId,
            String objectTitle) {
        String idempotencyKey = buildIdempotencyKey(actionType, objectId, recipientUserId);

        return builder(List.of(recipientUserId), actorUserIds, actionType, objectType)
                .objectId(objectId)
                .objectTitle(objectTitle)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    /**
     * Creates a NotificationEventMessage for multiple recipients.
     * Each recipient will have a unique idempotency key.
     *
     * @param recipientUserIds list of recipient user IDs
     * @param actorUserIds     list of actor user IDs
     * @param actionType       the action type
     * @param objectType       the object type
     * @param objectId         the object ID
     * @param objectTitle      the object title
     * @return a NotificationEventMessage (note: idempotency key should be set per recipient)
     */
    public static NotificationEventMessage createForMultipleRecipients(
            List<Long> recipientUserIds,
            List<Long> actorUserIds,
            NotificationActionType actionType,
            NotificationObjectType objectType,
            String objectId,
            String objectTitle) {
        // For multiple recipients, the idempotency key should be unique per recipient
        // This is a base message - producers should create separate messages per recipient
        // or use a composite idempotency key format
        String baseIdempotencyKey = String.format("%s:%s", actionType.getValue(), objectId);

        return builder(recipientUserIds, actorUserIds, actionType, objectType)
                .objectId(objectId)
                .objectTitle(objectTitle)
                .idempotencyKey(baseIdempotencyKey)
                .build();
    }
}

