package com.alexleong.libraryservice.borrower;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Registered borrower")
public record BorrowerResponse(
        @Schema(description = "Server-generated opaque borrower ID") UUID id,
        String name,
        String email) {
    static BorrowerResponse from(Borrower borrower) {
        return new BorrowerResponse(borrower.getId(), borrower.getName(), borrower.getEmail());
    }
}