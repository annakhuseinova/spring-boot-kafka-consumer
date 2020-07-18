package com.annakhuseinova.springbootkafkaconsumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Book {

    private Long bookId;
    private String bookName;
    private String bookAuthor;
}
