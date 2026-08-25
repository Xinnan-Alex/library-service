package com.alexleong.libraryservice.borrower;

import com.alexleong.libraryservice.loan.LoanResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "A registered borrower with complete loan history")
public record BorrowerHistoryResponse(
        UUID id,
        String name,
        String email,
        @Schema(description = "Loans in chronological order, including active and returned loans")
        List<LoanResponse> borrowHistory
) {
    static BorrowerHistoryResponse from(Borrower borrower, List<LoanResponse> borrowHistory) {
        return new BorrowerHistoryResponse(borrower.getId(), borrower.getName(), borrower.getEmail(), borrowHistory);
    }
}
