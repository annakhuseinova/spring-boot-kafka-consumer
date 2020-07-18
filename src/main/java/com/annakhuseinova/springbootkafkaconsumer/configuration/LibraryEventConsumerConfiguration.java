package com.annakhuseinova.springbootkafkaconsumer.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

// @EnableKafka is needed because we want to leverage spring boot autoconfiguration - automatic setting of KafkaConsumer
// from our properties file.
@Configuration
@EnableKafka
public class LibraryEventConsumerConfiguration {


}
