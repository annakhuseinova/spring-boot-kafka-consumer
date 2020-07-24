package com.annakhuseinova.springbootkafkaconsumer.consumer;

import com.annakhuseinova.springbootkafkaconsumer.service.LibraryEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LibraryEventsConsumer {

    private final LibraryEventsService libraryEventsService;

    // Обязательно указать к какой группе относится консьюмер
    // KafkaListener Annotation uses concurrent MessageListenerContainer.
    @KafkaListener(topics = {"libraryEvents"}, groupId = "library-events-listener-group")
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        log.info("Consumer Record: {}", consumerRecord);
        libraryEventsService.processLibrary(consumerRecord);
    }
}
