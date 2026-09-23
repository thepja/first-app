package com.example.app.book;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catégorie de référence (données gérées par les migrations Flyway, en lecture seule ici). */
@Entity
@Table(name = "category")
class Category {

    @Id
    private String code;

    private String label;

    private short position;

    protected Category() {}

    CategoryResponse toResponse() {
        return new CategoryResponse(code, label);
    }

    record CategoryResponse(String code, String label) {}
}
