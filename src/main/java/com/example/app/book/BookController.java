package com.example.app.book;

import com.example.app.account.CurrentUser;
import com.example.app.web.ApiException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Livres lus par l'utilisateur connecté. Chaque requête est limitée aux livres de cet utilisateur : un livre d'un autre
 * compte répond 404, comme s'il n'existait pas.
 */
@RestController
@RequestMapping("/api/books")
class BookController {

    private final BookRepository books;

    BookController(BookRepository books) {
        this.books = books;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<BookResponse> list(@AuthenticationPrincipal CurrentUser user) {
        return books.findAllByOwner(user.id()).stream().map(Book::toResponse).toList();
    }

    @PostMapping
    @Transactional
    ResponseEntity<BookResponse> create(
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody BookRequest body) {
        BookResponse created = books.saveAndFlush(new Book(user.id(), body)).toResponse();
        return ResponseEntity.created(URI.create("/api/books/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Transactional
    BookResponse update(
            @AuthenticationPrincipal CurrentUser user, @PathVariable long id, @Valid @RequestBody BookRequest body) {
        Book book = find(user, id);
        book.apply(body);
        return books.saveAndFlush(book).toResponse();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable long id) {
        books.delete(find(user, id));
    }

    private Book find(CurrentUser user, long id) {
        return books.findByIdAndOwnerId(id, user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Livre introuvable."));
    }
}
