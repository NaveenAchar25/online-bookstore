package com.bookstore.backend.repository;

import com.bookstore.backend.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest spins up an in-memory H2 instance and only the JPA layer,
 * verifying entity mapping and repository behavior against a real database
 * engine rather than a mock.
 */
@DataJpaTest
@ActiveProfiles({"dev", "test"})
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void savesAndRetrievesBook() {
        Book book = Book.builder()
                .title("Effective Java")
                .author("Joshua Bloch")
                .price(new BigDecimal("42.50"))
                .stockQuantity(5)
                .isbn("9780134685991")
                .build();

        Book saved = bookRepository.save(book);

        assertThat(saved.getId()).isNotNull();
        Optional<Book> found = bookRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Effective Java");
    }

    @Test
    void findAll_returnsAllPersistedBooks() {
        bookRepository.save(Book.builder().title("Book A").author("Author A")
                .price(BigDecimal.TEN).stockQuantity(1).build());
        bookRepository.save(Book.builder().title("Book B").author("Author B")
                .price(BigDecimal.ONE).stockQuantity(2).build());

        List<Book> all = bookRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    void auditableCallbacks_populateTimestampsOnPersist() {
        Book book = Book.builder().title("Refactoring").author("Martin Fowler")
                .price(new BigDecimal("40.25")).stockQuantity(12).build();

        Book saved = bookRepository.save(book);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void version_startsAtZeroForANewlyPersistedBook() {
        Book book = Book.builder().title("Domain-Driven Design").author("Eric Evans")
                .price(new BigDecimal("45.00")).stockQuantity(8).build();

        Book saved = bookRepository.saveAndFlush(book);

        assertThat(saved.getVersion()).isZero();
    }
}
