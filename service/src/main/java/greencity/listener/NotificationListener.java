package greencity.listener;

import greencity.dto.notification.NotificationDto;
import greencity.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
public class NotificationListener {
    // Assuming a configuration class defines this constant
    public static final String FRIEND_REQUEST_QUEUE = "friend-request-notifications";

    // Used to handle database persistence via the service layer
    private final NotificationService notificationService;

    // Used to send real-time messages over the WebSocket/STOMP broker
    private final SimpMessagingTemplate websocketTemplate;

    @Autowired
    public NotificationListener(
        NotificationService notificationService,
        SimpMessagingTemplate websocketTemplate) {
        this.notificationService = notificationService;
        this.websocketTemplate = websocketTemplate;
    }

    /**
     * Consumes the message from the dedicated friend request notification queue.
     *
     * @param notificationPayload The map containing details about the friend
     *                            request event.
     */
    @RabbitListener(queues = FRIEND_REQUEST_QUEUE)
    public void receiveFriendRequestNotification(Map<String, Object> notificationPayload) {
        Long receiverId = ((Number) notificationPayload.get("receiverId")).longValue();
        String content = (String) notificationPayload.get("content");

        log.debug("--- Notification Received for User {} ---", receiverId);
        log.debug("Content: {}", content);

        // 1. PERSISTENCE: Save the notification to the database (Step 5 completed)
        notificationService.saveNotification(receiverId, content);

        // 2. REAL-TIME DELIVERY: Push the clean DTO over WebSocket

        // Create a DTO for the WebSocket payload (it won't have the DB 'id' yet, but
        // it's not needed for the real-time card)
        NotificationDto dto = NotificationDto.builder()
            .content(content)
            .sentAt(LocalDateTime.now())
            .isRead(false)
            .build();

        // The destination is a private topic specific to the receiving user
        String destination = "/topic/user/" + receiverId + "/notifications";

        websocketTemplate.convertAndSend(destination, dto);

        log.debug("Notification persisted and pushed to WebSocket destination: {}", destination);
    }
}
