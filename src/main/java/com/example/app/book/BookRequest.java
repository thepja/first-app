package com.example.app.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

record BookRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String author,
        @PastOrPresent LocalDate readOn,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 5000) String comment) {}
