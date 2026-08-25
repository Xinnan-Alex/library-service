package com.alexleong.libraryservice;

import com.alexleong.libraryservice.book.BookController;
import com.alexleong.libraryservice.book.BookResponse;
import com.alexleong.libraryservice.book.BookService;
import com.alexleong.libraryservice.borrower.BorrowerController;
import com.alexleong.libraryservice.borrower.BorrowerHistoryResponse;
import com.alexleong.libraryservice.borrower.BorrowerResponse;
import com.alexleong.libraryservice.borrower.BorrowerService;
import com.alexleong.libraryservice.error.ApiExceptionHandler;
import com.alexleong.libraryservice.error.ConflictException;
import com.alexleong.libraryservice.loan.LoanResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogControllerTest {
    private BorrowerService borrowerService;
    private BookService bookService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        borrowerService = mock(BorrowerService.class);
        bookService = mock(BookService.class);
        mvc = MockMvcBuilders.standaloneSetup(new BorrowerController(borrowerService), new BookController(bookService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void borrowerValidationReturnsStableJsonError() throws Exception {
        mvc.perform(post("/api/v1/borrowers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/borrowers"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void createsBorrower() throws Exception {
        UUID id = UUID.randomUUID();
        when(borrowerService.create(any())).thenReturn(new BorrowerResponse(id, "Alice", "alice@example.com"));

        mvc.perform(post("/api/v1/borrowers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void listsBorrowersWithLoanHistory() throws Exception {
        UUID borrowerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        LoanResponse loan = new LoanResponse(UUID.randomUUID(), bookId, borrowerId,
                Instant.parse("2026-08-25T10:00:00Z"), null);
        when(borrowerService.list()).thenReturn(List.of(
                new BorrowerHistoryResponse(borrowerId, "Alice", "alice@example.com", List.of(loan))));

        mvc.perform(get("/api/v1/borrowers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(borrowerId.toString()))
                .andExpect(jsonPath("$[0].borrowHistory[0].bookId").value(bookId.toString()))
                .andExpect(jsonPath("$[0].borrowHistory[0].returnedAt").doesNotExist());
    }

    @Test
    void bookValidationReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"\",\"title\":\"\",\"author\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.isbn").exists());
    }

    @Test
    void metadataConflictReturnsConflict() throws Exception {
        when(bookService.create(any())).thenThrow(new ConflictException("ISBN is already registered with different title or author"));

        mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9780306406157\",\"title\":\"Title\",\"author\":\"Author\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void listsEveryPhysicalCopy() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(bookService.list()).thenReturn(List.of(
                new BookResponse(first, "9780306406157", "Title", "Author", true),
                new BookResponse(second, "9780306406157", "Title", "Author", false)));

        mvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first.toString()))
                .andExpect(jsonPath("$[1].available").value(false));
    }
}