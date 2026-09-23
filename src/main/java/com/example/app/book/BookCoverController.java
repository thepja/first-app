package com.example.app.book;

import com.example.app.account.CurrentUser;
import com.example.app.web.ApiException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Couverture d'un livre : visible et modifiable uniquement par son propriétaire. */
@RestController
@RequestMapping("/api/books/{id}/cover")
class BookCoverController {

    private final BookRepository books;
    private final BookCoverRepository covers;
    private final CoverImages images;

    BookCoverController(BookRepository books, BookCoverRepository covers, CoverImages images) {
        this.books = books;
        this.covers = covers;
        this.images = images;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    BookResponse upload(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable long id,
            @RequestParam("file") MultipartFile file) {
        Book book = find(user, id);
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Aucune image reçue.");
        }
        byte[] jpeg = images.toJpeg(bytes(file));
        covers.findById(id).ifPresentOrElse(cover -> cover.replace(jpeg), () -> covers.save(new BookCover(id, jpeg)));
        book.coverChanged(Instant.now());
        return books.saveAndFlush(book).toResponse();
    }

    @DeleteMapping
    @Transactional
    ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable long id) {
        Book book = find(user, id);
        covers.deleteById(id);
        book.coverChanged(null);
        return ResponseEntity.noContent().build();
    }

    /** L'URL contient la date de mise à jour (?v=…) : l'image peut être gardée en cache sans limite. */
    @GetMapping
    @Transactional(readOnly = true)
    ResponseEntity<byte[]> get(@AuthenticationPrincipal CurrentUser user, @PathVariable long id) {
        find(user, id);
        BookCover cover =
                covers.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pas de couverture."));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(
                        CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(cover.jpeg());
    }

    private Book find(CurrentUser user, long id) {
        return books.findByIdAndOwnerId(id, user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Livre introuvable."));
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
