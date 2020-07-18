package com.annakhuseinova.springbootkafkaconsumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LibraryEventsConsumer {

    // KafkaListener Annotation uses concurrent MessageListenerContainer.
    @KafkaListener(topics = {"libraryEvents"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord){
        log.info("Consumer Record: {}", consumerRecord);
    }
}
