package com.bookstore.backend.controller;

import com.bookstore.backend.dto.BookDto;
import com.bookstore.backend.exception.BookNotFoundException;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.security.JwtService;
import com.bookstore.backend.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loads only the web layer, with BookService mocked — verifies the HTTP
 * contract (status codes, JSON shape) without touching a database.
 *
 * @WebMvcTest moved to org.springframework.boot.webmvc.test.autoconfigure
 * and @MockBean was removed entirely as of Spring Boot 4 — @MockitoBean
 * (Spring Framework 7's bean-override support) is the replacement.
 */
@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAllBooks_returns200AndBookList() throws Exception {
        BookDto book = BookDto.builder()
                .id(1L).title("Clean Code").author("Robert C. Martin")
                .price(new BigDecimal("35.99")).stockQuantity(10)
                .build();
        when(bookService.getAllBooks()).thenReturn(List.of(book));

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[0].author").value("Robert C. Martin"))
                .andExpect(jsonPath("$[0].price").value(35.99));
    }

    @Test
    void getAllBooks_emptyCatalog_returns200AndEmptyArray() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBookById_found_returns200AndBook() throws Exception {
        BookDto book = BookDto.builder()
                .id(1L).title("Clean Code").author("Robert C. Martin")
                .price(new BigDecimal("35.99")).stockQuantity(10)
                .build();
        when(bookService.getBookById(1L)).thenReturn(book);

        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void getBookById_notFound_returns404WithErrorBody() throws Exception {
        when(bookService.getBookById(anyLong())).thenThrow(new BookNotFoundException(999L));

        mockMvc.perform(get("/api/v1/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Book not found with id: 999"));
    }

    @Test
    void getBookById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books/abc"))
                .andExpect(status().isBadRequest());
    }
}
