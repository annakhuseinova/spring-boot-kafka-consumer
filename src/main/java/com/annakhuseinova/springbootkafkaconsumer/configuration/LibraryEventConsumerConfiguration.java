package com.annakhuseinova.springbootkafkaconsumer.configuration;

import com.annakhuseinova.springbootkafkaconsumer.service.LibraryEventsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// @EnableKafka is needed because we want to leverage spring boot autoconfiguration - automatic setting of KafkaConsumer
// from our properties file.
@Configuration
@EnableKafka
@Slf4j
@RequiredArgsConstructor
public class LibraryEventConsumerConfiguration {

    private final LibraryEventsService libraryEventsService;

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializerClass;
    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Integer, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Integer, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Creating lambda for custom error handling
        factory.setErrorHandler(((thrownException, data) -> {
            log.info("Exception in consumerConfig is {} and the record is {}", thrownException.getMessage(), data);
        }));
        factory.setRetryTemplate(retryTemplate());
        // Recovery Settings
        factory.setRecoveryCallback(context -> {
            if (context.getLastThrowable().getCause() instanceof RecoverableDataAccessException){
                log.debug("Inside recoverable logic");
                Arrays.asList(context.attributeNames()).forEach(attributeName -> {
                    log.info("Attribute name is :  {}", attributeName);
                    log.info("Attribute Value is {}", context.getAttribute(attributeName));
                    //  record - по этому аттрибуту можно получить проблемную ConsumerRecord.
                    // consumer - KafkaConsumer - который получает плохие сообщения.
                    // exhausted - признак того, что retries кончились.
                    ConsumerRecord<Integer, String> consumerRecord = (ConsumerRecord<Integer, String>) context.getAttribute("record");
                    libraryEventsService.handleRecovery(consumerRecord);
                });
            }else {
                log.debug("Inside non-recoverable logic");
                throw new RuntimeException(context.getLastThrowable().getMessage());
            }
            return null;
        });
        return factory;
    }

    // Back off - period after each retry
    // Для retries устанавливаем простую retry policy, которая лишь трижды пытается обработать сообщение.
    private RetryTemplate retryTemplate() {

        // Default is 1000 milliseconds
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(1000);
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(simpleRetryPolicy());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        return retryTemplate;
    }
    // Первый вариант - создание простой логики, где при ошибке consumer пытается 3 раза повторить получение сообщения.
    // Второй вариант - retry policy отрабатывает при выбросе определенного типа исключения. Вторым параметром
    // SimpleRetryPolicy конструктора передаем словарь исключений, на которые хотим и не хотим вызывать Retry Policy.
    private RetryPolicy simpleRetryPolicy() {

        Map<Class<? extends Throwable>, Boolean> exceptionMap = new HashMap<>();
        exceptionMap.put(IllegalArgumentException.class, false);
        exceptionMap.put(RecoverableDataAccessException.class, true);
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(3, exceptionMap, true);
//
//        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
//        simpleRetryPolicy.setMaxAttempts(3);
        return simpleRetryPolicy;
    }

    @Bean
    public ConsumerFactory<Integer, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        return props;
    }

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker(){
        return new EmbeddedKafkaBroker(3);
    }
}
