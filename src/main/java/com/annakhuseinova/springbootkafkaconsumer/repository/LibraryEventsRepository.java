package com.annakhuseinova.springbootkafkaconsumer.repository;

import com.annakhuseinova.springbootkafkaconsumer.model.LibraryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryEventsRepository extends JpaRepository<LibraryEvent, Integer> {

    List<LibraryEvent> findAll();
}
