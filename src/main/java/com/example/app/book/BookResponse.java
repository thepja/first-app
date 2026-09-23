package com.example.app.book;

import java.time.Instant;
import java.time.LocalDate;

record BookResponse(
        long id,
        String title,
        String author,
        LocalDate readOn,
        int rating,
        String comment,
        String coverUrl,
        Instant createdAt,
        Instant updatedAt) {}
