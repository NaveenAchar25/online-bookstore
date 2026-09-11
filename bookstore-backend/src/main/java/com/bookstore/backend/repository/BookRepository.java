package com.bookstore.backend.repository;

import com.bookstore.backend.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    // findAll(Pageable) is inherited from JpaRepository for the no-search browsing case.
    // This derived query method covers search: matches
    // either the title or the author, case-insensitively, substring match.
    Page<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
            String title, String author, Pageable pageable);
}
