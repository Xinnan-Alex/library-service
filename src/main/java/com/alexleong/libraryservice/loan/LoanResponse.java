package com.alexleong.libraryservice.loan;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Historical loan lifecycle")
public record LoanResponse(
        UUID id,
        UUID bookId,
        UUID borrowerId,
        Instant borrowedAt,
        @Schema(description = "Return time, or null while the loan is active", nullable = true) Instant returnedAt
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getBookCopy().getId(), loan.getBorrower().getId(),
                loan.getBorrowedAt(), loan.getReturnedAt());
    }
}
