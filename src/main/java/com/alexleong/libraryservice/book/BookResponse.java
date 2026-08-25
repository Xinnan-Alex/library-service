package com.alexleong.libraryservice.book;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A registered physical book copy with flattened bibliographic metadata")
public record BookResponse(
        @Schema(description = "Server-generated opaque physical-copy ID") UUID id,
        @Schema(description = "Normalized ISBN without separators") String isbn,
        String title,
        String author,
        @Schema(description = "True when the copy has no active loan") boolean available) {
    static BookResponse from(BookCopy copy, boolean available) {
        BookRecord record = copy.getBookRecord();
        return new BookResponse(copy.getId(), record.getIsbn(), record.getTitle(), record.getAuthor(), available);
    }
}