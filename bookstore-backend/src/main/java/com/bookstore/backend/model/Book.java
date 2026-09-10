package com.bookstore.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A book available for sale in the catalog. Kept as a persistence-layer
 * entity only — it is never returned directly from a controller. That job
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(length = 20)
    private String isbn;

    @Column(length = 1000)
    private String description;

    /**
     * Optimistic-locking version. JPA increments this automatically on every
     * the mechanism on which checkout feature relies to detect two customers
     * concurrently buying the last copy of a book, to avoid overselling.
     */
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    /**
     * Whether this book currently has enough stock to satisfy the given
     * quantity. A method on Book itself, not a raw field
     */
    public boolean hasSufficientStock(int requestedQuantity) {
        return stockQuantity >= requestedQuantity;
    }
}
