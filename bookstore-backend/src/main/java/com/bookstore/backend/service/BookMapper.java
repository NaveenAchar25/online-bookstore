package com.bookstore.backend.service;

import com.bookstore.backend.dto.BookDto;
import com.bookstore.backend.model.Book;
import org.springframework.stereotype.Component;


@Component
public class BookMapper {

    public BookDto toDto(Book book) {
        return BookDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .price(book.getPrice())
                .stockQuantity(book.getStockQuantity())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .build();
    }
}
