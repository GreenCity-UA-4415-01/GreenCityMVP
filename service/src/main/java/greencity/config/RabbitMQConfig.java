package greencity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String FRIEND_REQUEST_QUEUE = "friend-request-notifications";
    public static final String FRIEND_REQUEST_EXCHANGE = "friend-request-exchange";
    public static final String ROUTING_KEY = "friend.request.key";

    // 1. Declare the Queue
    @Bean
    public Queue friendRequestQueue() {
        // The queue is durable (survives broker restart)
        return new Queue(FRIEND_REQUEST_QUEUE, true);
    }

    // 2. Declare the Exchange (using Direct for specific routing)
    @Bean
    public DirectExchange friendRequestExchange() {
        return new DirectExchange(FRIEND_REQUEST_EXCHANGE, true, false);
    }

    // 3. Bind the Queue to the Exchange using the Routing Key
    @Bean
    public Binding friendRequestBinding(Queue friendRequestQueue, DirectExchange friendRequestExchange) {
        return BindingBuilder.bind(friendRequestQueue).to(friendRequestExchange).with(ROUTING_KEY);
    }

    // 4. Configure Message Converter (to serialize/deserialize Java objects to
    // JSON)
    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        // FIX: The default ObjectMapper does not support Java 8 Date/Time
        // (LocalDateTime).
        // We must create and configure a custom ObjectMapper with the JavaTimeModule.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
