package com.example.app.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "book")
class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(name = "read_on")
    private LocalDate readOn;

    @Column(nullable = false)
    private short rating;

    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cover_updated_at")
    private Instant coverUpdatedAt;

    protected Book() {}

    Book(long ownerId, BookRequest request) {
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        apply(request);
    }

    void apply(BookRequest request) {
        this.title = request.title().strip();
        this.author = blankToNull(request.author());
        this.readOn = request.readOn();
        this.rating = request.rating().shortValue();
        this.comment = blankToNull(request.comment());
    }

    /** Date de la dernière couverture envoyée, ou null si le livre n'en a plus. */
    void coverChanged(Instant when) {
        this.coverUpdatedAt = when;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    BookResponse toResponse() {
        String coverUrl =
                coverUpdatedAt == null ? null : "/api/books/" + id + "/cover?v=" + coverUpdatedAt.toEpochMilli();
        return new BookResponse(id, title, author, readOn, rating, comment, coverUrl, createdAt, updatedAt);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
