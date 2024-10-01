package com.sellersphere.orderservice.invalidation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

@Component
public final class KafkaOrderInvalidationService implements OrderInvalidationService {

    private final String topic;
    private final KafkaTemplate<String, String> kafka;

    public KafkaOrderInvalidationService(KafkaTemplate<String, String> kafka, @Value("${kafka.topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void schedule(String userId, String orderId, ZonedDateTime dateTime) {
        String data = """
                { "userId": "%s", "orderId": "%s", "dateTime": "%s" }
                """.formatted(userId, orderId, dateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        try {
            kafka.send(topic, data).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Could not send order placed event in topic " + topic, e);
        }
    }
}
