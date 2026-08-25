package com.alexleong.libraryservice.loan;

import com.alexleong.libraryservice.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Loans", description = "Borrow and return physical copies")
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/loans")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Borrow a physical copy", description = "Creates an active loan immediately for the supplied borrower and copy IDs.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed IDs",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Borrower or copy not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Copy already has an active loan",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LoanResponse borrow(@Valid @RequestBody CreateLoanRequest request) {
        return service.borrow(request);
    }

    @PostMapping("/api/v1/books/{bookId}/return")
    @Operation(summary = "Return a physical copy", description = "Closes the copy's active loan immediately while retaining its history.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active loan closed"),
            @ApiResponse(responseCode = "400", description = "Malformed book ID",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Copy not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Copy has no active loan",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LoanResponse returnBook(@PathVariable UUID bookId) {
        return service.returnBook(bookId);
    }
}
