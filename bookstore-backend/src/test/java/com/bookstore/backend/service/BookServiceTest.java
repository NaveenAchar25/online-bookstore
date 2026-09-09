package com.bookstore.backend.service;

import com.bookstore.backend.dto.BookDto;
import com.bookstore.backend.exception.BookNotFoundException;
import com.bookstore.backend.model.Book;
import com.bookstore.backend.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
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

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, new BookMapper());

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
    @DisplayName("getAllBooks() returns every book mapped to a DTO")
    void getAllBooks_returnsMappedList() {
        when(bookRepository.findAll()).thenReturn(List.of(sampleBook));

        List<BookDto> result = bookService.getAllBooks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.get(0).getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.get(0).getPrice()).isEqualByComparingTo("35.99");
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("getAllBooks() returns an empty list when the catalog is empty")
    void getAllBooks_emptyCatalog_returnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<BookDto> result = bookService.getAllBooks();

        assertThat(result).isEmpty();
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
