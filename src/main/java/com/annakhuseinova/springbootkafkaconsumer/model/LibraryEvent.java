package com.annakhuseinova.springbootkafkaconsumer.model;

import lombok.*;

import javax.persistence.*;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class LibraryEvent {

    @Id
    @GeneratedValue
    private Integer libraryEventId;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "libraryEvent")
    // Exclude используется для того, чтобы избежать проблемы циклических ссылок при генерации метод toString()
    @ToString.Exclude
    private Book book;
    @Enumerated(EnumType.STRING)
    private LibraryEventType libraryEventType;
}
