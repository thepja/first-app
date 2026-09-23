package com.example.app.book;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookCoverRepository extends JpaRepository<BookCover, Long> {}
