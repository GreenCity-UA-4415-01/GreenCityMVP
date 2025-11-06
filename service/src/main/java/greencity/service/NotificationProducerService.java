package greencity.service;

import greencity.constant.NotificationRoutingKeys;
import greencity.message.NotificationEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationProducerService {
    private final RabbitTemplate rabbitTemplate;

    public void sendNotificationEvent(NotificationEventMessage event, String routingKey) {
        rabbitTemplate.convertAndSend(
                NotificationRoutingKeys.NOTIFICATIONS_EXCHANGE,
                routingKey,
                event
        );
        log.info("Published notification event: key={}, event={}", routingKey, event);
    }
}