package greencity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static greencity.constant.NotificationRoutingKeys.*;

/**
 * RabbitMQ configuration for notification events.
 * Configures exchange, queues, dead letter queue, and message converters.
 */
@Configuration
@EnableRabbit
@Slf4j
public class RabbitMQConfig {

    /**
     * Creates the topic exchange for notification events.
     *
     * @return TopicExchange for notifications
     */
    @Bean
    public TopicExchange notificationsExchange() {
        TopicExchange exchange = new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
        log.info("Created notifications exchange: {}", NOTIFICATIONS_EXCHANGE);
        return exchange;
    }

    /**
     * Creates the main queue for notification write operations.
     *
     * @return Queue for notifications
     */
    @Bean
    public Queue notificationsWriteQueue() {
        Queue queue = QueueBuilder.durable(NOTIFICATIONS_WRITE_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATIONS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", NOTIFICATIONS_WRITE_QUEUE + ".dlq")
                .build();
        log.info("Created notifications write queue: {}", NOTIFICATIONS_WRITE_QUEUE);
        return queue;
    }

    /**
     * Creates the dead letter exchange for failed messages.
     *
     * @return TopicExchange for dead letters
     */
    @Bean
    public TopicExchange notificationsDlxExchange() {
        TopicExchange exchange = new TopicExchange(NOTIFICATIONS_EXCHANGE + ".dlx", true, false);
        log.info("Created notifications DLX exchange: {}", NOTIFICATIONS_EXCHANGE + ".dlx");
        return exchange;
    }

    /**
     * Creates the dead letter queue for failed notification messages.
     *
     * @return Queue for dead letters
     */
    @Bean
    public Queue notificationsDlq() {
        Queue queue = QueueBuilder.durable(NOTIFICATIONS_WRITE_QUEUE + ".dlq").build();
        log.info("Created notifications DLQ: {}", NOTIFICATIONS_WRITE_QUEUE + ".dlq");
        return queue;
    }

    /**
     * Binds the notifications write queue to the exchange with routing pattern.
     * Uses wildcard pattern to match all notification routing keys.
     *
     * @return Binding
     */
    @Bean
    public Binding notificationsWriteBinding() {
        Binding binding = BindingBuilder
                .bind(notificationsWriteQueue())
                .to(notificationsExchange())
                .with("#"); // Match all routing keys
        log.info("Bound {} to {} with pattern: #", NOTIFICATIONS_WRITE_QUEUE, NOTIFICATIONS_EXCHANGE);
        return binding;
    }

    /**
     * Binds the dead letter queue to the dead letter exchange.
     *
     * @return Binding
     */
    @Bean
    public Binding notificationsDlqBinding() {
        Binding binding = BindingBuilder
                .bind(notificationsDlq())
                .to(notificationsDlxExchange())
                .with(NOTIFICATIONS_WRITE_QUEUE + ".dlq");
        log.info("Bound DLQ to DLX");
        return binding;
    }

    /**
     * Configures JSON message converter for RabbitMQ.
     *
     * @return MessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures RabbitTemplate with JSON converter.
     *
     * @param connectionFactory the connection factory
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Configures RabbitListenerContainerFactory with JSON converter and error handling.
     *
     * @param connectionFactory the connection factory
     * @return SimpleRabbitListenerContainerFactory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false); // Don't requeue rejected messages, send to DLQ
        return factory;
    }
}

