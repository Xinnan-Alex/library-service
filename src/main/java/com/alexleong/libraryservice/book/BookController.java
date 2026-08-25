package com.alexleong.libraryservice.book;

import com.alexleong.libraryservice.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Physical book-copy inventory")
public class BookController {
    private final BookService service;
    public BookController(BookService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a physical book copy", description = "Accepts ISBN-10 or ISBN-13 and returns a distinct copy ID. Existing ISBN metadata must match.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Copy registered"),
            @ApiResponse(responseCode = "400", description = "Invalid ISBN, metadata, or request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "The ISBN is already registered with different title or author",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public BookResponse create(@Valid @RequestBody CreateBookRequest request) { return service.create(request); }

    @GetMapping
    @Operation(summary = "List all physical book copies", description = "Returns every registered copy, including separate IDs sharing an ISBN, with current availability.")
    @ApiResponse(responseCode = "200", description = "Inventory returned")
    public List<BookResponse> list() { return service.list(); }
}