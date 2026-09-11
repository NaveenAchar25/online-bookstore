package com.bookstore.backend.controller;

import com.bookstore.backend.dto.BookDto;
import com.bookstore.backend.dto.PagedResponse;
import com.bookstore.backend.service.BookService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public catalog endpoints — a customer can browse and view books without any authentication. 
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * ?q= is optional — or blank browses the whole catalog page by
     * page. Pageable binds ?page=, ?size=, and ?sort= automatically
     * PageableDefault caps the default page size — without an explicit upper bound here
     */
    @GetMapping
    public ResponseEntity<PagedResponse<BookDto>> getAllBooks(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 12, sort = "id") Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(bookService.getAllBooks(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(bookService.getBookById(id));
    }
}
