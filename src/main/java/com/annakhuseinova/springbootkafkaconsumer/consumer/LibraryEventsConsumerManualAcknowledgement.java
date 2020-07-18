package com.annakhuseinova.springbootkafkaconsumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

// We need to implement this interface for being able to use manual acknowledgement.
@Component
@Slf4j
public class LibraryEventsConsumerManualAcknowledgement implements AcknowledgingMessageListener<Integer, String> {


    @Override
    @KafkaListener(topics = {"libraryEvents"})
    public void onMessage(ConsumerRecord<Integer, String> data, Acknowledgment acknowledgment) {
        log.info("Consumer Record: {}", data);
        // Means to say that we have successfully processed the message.
        acknowledgment.acknowledge();
    }
}