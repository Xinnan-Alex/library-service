package com.alexleong.libraryservice.loan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Identifiers needed to borrow a physical copy")
public record CreateLoanRequest(
        @Schema(description = "Physical-copy ID returned by the books API") @NotNull UUID bookId,
        @Schema(description = "Borrower ID returned by the borrowers API") @NotNull UUID borrowerId
) { }
