package com.annakhuseinova.springbootkafkaconsumer.service;

import com.annakhuseinova.springbootkafkaconsumer.model.LibraryEvent;
import com.annakhuseinova.springbootkafkaconsumer.repository.LibraryEventsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryEventsService {

    private final ObjectMapper objectMapper;
    private final LibraryEventsRepository libraryEventsRepository;

    public void processLibrary(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent.class);
        log.info("libraryEvent: {}", libraryEvent);

        if (libraryEvent.getLibraryEventId() != null && libraryEvent.getLibraryEventId() == 0){
            throw new RecoverableDataAccessException("Temporary Network Issue");
        }
        switch (libraryEvent.getLibraryEventType()){
            case NEW:
                save(libraryEvent);
                break;
            case UPDATE:
                validate(libraryEvent);
                save(libraryEvent);
                break;
            default:
                log.info("Invalid Library Event Type");
        }
    }

    private void validate(LibraryEvent libraryEvent) {
        if (libraryEvent.getLibraryEventId() == null){
            throw new IllegalArgumentException("Library Event Id is missing");
        }
        Optional<LibraryEvent> libraryEventsOptional = libraryEventsRepository.findById(libraryEvent.getLibraryEventId());
        if (!libraryEventsOptional.isPresent()){
            throw new IllegalArgumentException("Not a valid library event");
        }
        log.info("Validation is successful for the library event : {}", libraryEventsOptional.get());
    }

    private void save(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);
        log.info("Successfully persisted the library event", libraryEvent);
    }
}
