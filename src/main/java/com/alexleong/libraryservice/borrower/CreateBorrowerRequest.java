package com.alexleong.libraryservice.borrower;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Borrower registration details")
public record CreateBorrowerRequest(
        @Schema(example = "John Doe") @NotBlank @Size(max = 200) String name,
        @Schema(example = "ada@example.com") @NotBlank @Email @Size(max = 320) String email) {
}