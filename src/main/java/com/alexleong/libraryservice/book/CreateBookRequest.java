package com.alexleong.libraryservice.book;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Physical book-copy registration details")
public record CreateBookRequest(
        @Schema(description = "ISBN-10 or ISBN-13; spaces and hyphens are accepted", example = "978-0132350884")
        @NotBlank @Size(max = 32) String isbn,
        @Schema(example = "Clean Code: A Handbook of Agile Software Craftsmanship") @NotBlank @Size(max = 500) String title,
        @Schema(example = "Robert Cecil Martin") @NotBlank @Size(max = 300) String author) {
}