package com.alexleong.libraryservice.borrower;

import com.alexleong.libraryservice.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/borrowers")
@Tag(name = "Borrowers", description = "Borrower registration and loan history")
public class BorrowerController {
    private final BorrowerService service;

    public BorrowerController(BorrowerService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a borrower", description = "Creates a borrower with a server-generated ID. Email addresses need not be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Borrower registered"),
            @ApiResponse(responseCode = "400", description = "Invalid name, email, or request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public BorrowerResponse create(@Valid @RequestBody CreateBorrowerRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "List borrowers and loan history", description = "Returns every borrower with active and returned loans in chronological order.")
    @ApiResponse(responseCode = "200", description = "Borrowers and loan histories returned")
    public List<BorrowerHistoryResponse> list() {
        return service.list();
    }
}