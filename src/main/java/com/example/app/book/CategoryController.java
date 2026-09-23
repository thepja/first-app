package com.example.app.book;

import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
class CategoryController {

    private final CategoryRepository categories;

    CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    /** Liste dans l'ordre d'affichage ; elle ne change qu'au déploiement, d'où un cache d'une heure. */
    @GetMapping
    @Transactional(readOnly = true)
    ResponseEntity<List<Category.CategoryResponse>> list() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(categories.findAllByOrderByPositionAsc().stream()
                        .map(Category::toResponse)
                        .toList());
    }
}
