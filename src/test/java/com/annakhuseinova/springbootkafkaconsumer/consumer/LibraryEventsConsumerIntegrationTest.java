package com.annakhuseinova.springbootkafkaconsumer.consumer;

import com.annakhuseinova.springbootkafkaconsumer.model.Book;
import com.annakhuseinova.springbootkafkaconsumer.model.LibraryEvent;
import com.annakhuseinova.springbootkafkaconsumer.model.LibraryEventType;
import com.annakhuseinova.springbootkafkaconsumer.repository.LibraryEventsRepository;
import com.annakhuseinova.springbootkafkaconsumer.service.LibraryEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
"spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
public class LibraryEventsConsumerIntegrationTest {

    private EmbeddedKafkaBroker embeddedKafkaBroker;
    private KafkaTemplate<Integer, String> kafkaTemplate;
    /**
     *  Creates the necessary MessageListenerContainer instances
     *  for the registered KafkaListenerEndpoint. Also manages the lifecycle of the listener containers in particular
     *  within the lifecycle of the application context.
     * */
    private KafkaListenerEndpointRegistry endpointRegistry;
    private ObjectMapper objectMapper;

    @SpyBean
    private LibraryEventsService libraryEventsServiceSpy;

    @SpyBean
    private LibraryEventsConsumer libraryEventsConsumerSpy;

    private LibraryEventsRepository libraryEventsRepository;

    @Autowired
    public void setLibraryEventsRepository(LibraryEventsRepository libraryEventsRepository) {
        this.libraryEventsRepository = libraryEventsRepository;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    public void setEndpointRegistry(KafkaListenerEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @Autowired
    public void setEmbeddedKafkaBroker(EmbeddedKafkaBroker embeddedKafkaBroker) {
        this.embeddedKafkaBroker = embeddedKafkaBroker;
    }

    @Autowired
    public void setKafkaTemplate(KafkaTemplate<Integer, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @BeforeEach
    void setUp() {
        endpointRegistry.getListenerContainers().forEach(messageListenerContainer -> {
            // Utilities for testing listener containers.
            // waitForAssignment - wait until the container has the required number of assigned partitions.
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        });
    }

    @AfterEach
    void tearDown() {
        libraryEventsRepository.deleteAll();
    }

    @Test
    void  publishNewLibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException {

        LibraryEvent testLibraryEvent = LibraryEvent.builder()
                .book(new Book(456L, "Author", "Title",
                        new LibraryEvent(null, null ,LibraryEventType.NEW)))
                .libraryEventType(LibraryEventType.NEW)
                .libraryEventId(null)
                .build();

        kafkaTemplate.sendDefault(objectMapper.writeValueAsString(testLibraryEvent)).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).processLibrary(isA(ConsumerRecord.class));

        List<LibraryEvent> libraryEvents = libraryEventsRepository.findAll();

        assert libraryEvents.size() == 1;
        libraryEvents.forEach(libraryEvent -> {
            assert libraryEvent.getLibraryEventId() != null;
            assertEquals(456L, libraryEvent.getBook().getBookId());
        });
    }

    @Test
    void publishUpdateLibraryEvent() throws InterruptedException, JsonProcessingException, ExecutionException {

        // given
        String json = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka" +
                " Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
        LibraryEvent libraryEvent = objectMapper.readValue(json, LibraryEvent.class);
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);

        Book updatedBook = Book.builder()
                .bookId(456l)
                .bookName("Kafka Using Spring Boot 2.x")
                .bookAuthor("SomeName")
                .build();
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEvent.setBook(updatedBook);
        String updatedJson = objectMapper.writeValueAsString(libraryEvent);
        kafkaTemplate.sendDefault(libraryEvent.getLibraryEventId(), updatedJson).get();
        // when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        // then
        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).processLibrary(isA(ConsumerRecord.class));

        LibraryEvent persistedLibraryEvent = libraryEventsRepository.findById(libraryEvent.getLibraryEventId()).get();
        assertEquals("Kafka Using Spring Boot 2.x", persistedLibraryEvent.getBook().getBookName());
    }
}
