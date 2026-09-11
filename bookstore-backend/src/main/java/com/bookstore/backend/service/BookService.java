package com.bookstore.backend.service;

import com.bookstore.backend.dto.BookDto;
import com.bookstore.backend.dto.PagedResponse;
import com.bookstore.backend.exception.BookNotFoundException;
import com.bookstore.backend.model.Book;
import com.bookstore.backend.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper mapper;

    public BookService(BookRepository bookRepository, BookMapper mapper) {
        this.bookRepository = bookRepository;
        this.mapper = mapper;
    }

    /**
     * A blank or absent query browses the full catalog page by page; a
     * non-blank one searches title and author. Two different repository
     * calls rather than one query with a "match everything" fallback
     * pattern — the plain findAll(Pageable) path stays a simple, fast,
     * fully-indexed lookup for the common case of just browsing.
     */
    public PagedResponse<BookDto> getAllBooks(String query, Pageable pageable) {
        Page<Book> page = (query == null || query.isBlank())
                ? bookRepository.findAll(pageable)
                : bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query, pageable);

        return PagedResponse.<BookDto>builder()
                .items(page.getContent().stream().map(mapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public BookDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        return mapper.toDto(book);
    }
}
