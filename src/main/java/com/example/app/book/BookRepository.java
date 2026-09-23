package com.example.app.book;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface BookRepository extends JpaRepository<Book, Long> {

    /** Lectures les plus récentes d'abord, livres sans date de lecture en dernier. */
    @Query("select b from Book b where b.ownerId = :ownerId order by b.readOn desc nulls last, b.id desc")
    List<Book> findAllByOwner(long ownerId);

    Optional<Book> findByIdAndOwnerId(long id, long ownerId);
}
