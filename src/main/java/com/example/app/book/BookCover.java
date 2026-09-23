package com.example.app.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_cover")
class BookCover {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false)
    private byte[] data;

    protected BookCover() {}

    BookCover(long bookId, byte[] jpeg) {
        this.bookId = bookId;
        this.data = jpeg;
    }

    void replace(byte[] jpeg) {
        this.data = jpeg;
    }

    byte[] jpeg() {
        return data;
    }
}
