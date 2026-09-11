package com.bookstore.backend.service;

import com.bookstore.backend.dto.BookDto;
import com.bookstore.backend.dto.PagedResponse;
import com.bookstore.backend.exception.BookNotFoundException;
import com.bookstore.backend.model.Book;
import com.bookstore.backend.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Repository is mocked, so these run fast and verify BookService's own
 * behavior in isolation from any real database.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    private Book sampleBook;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, new BookMapper());
        pageable = PageRequest.of(0, 12);

        sampleBook = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(new BigDecimal("35.99"))
                .stockQuantity(10)
                .isbn("9780132350884")
                .description("A handbook of agile software craftsmanship")
                .build();
    }

    @Test
    @DisplayName("getAllBooks() with no query browses via plain findAll(Pageable)")
    void getAllBooks_noQuery_usesPlainFindAll() {
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleBook), pageable, 1));

        PagedResponse<BookDto> result = bookService.getAllBooks(null, pageable);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isZero();
    }

    @Test
    @DisplayName("getAllBooks() with a blank query also browses, not searches")
    void getAllBooks_blankQuery_usesPlainFindAll() {
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleBook), pageable, 1));

        bookService.getAllBooks("   ", pageable);

        // No exception, no call to the search method — verified implicitly:
        // Mockito would fail this test with an UnnecessaryStubbingException
        // in strict mode if findAll(Pageable) were never actually invoked.
    }

    @Test
    @DisplayName("getAllBooks() with a query searches title and author")
    void getAllBooks_withQuery_usesSearchMethod() {
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("clean", "clean", pageable))
                .thenReturn(new PageImpl<>(List.of(sampleBook), pageable, 1));

        PagedResponse<BookDto> result = bookService.getAllBooks("clean", pageable);

        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getAllBooks() returns an empty page when nothing matches")
    void getAllBooks_noMatches_returnsEmptyPage() {
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PagedResponse<BookDto> result = bookService.getAllBooks(null, pageable);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("getBookById() returns the matching book when it exists")
    void getBookById_found_returnsDto() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        BookDto result = bookService.getBookById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("getBookById() throws BookNotFoundException when no book matches")
    void getBookById_notFound_throws() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(999L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("999");
    }
}
