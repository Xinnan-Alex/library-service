package com.alexleong.libraryservice.loan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoanIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private LoanRepository loanRepository;

    @Test
    void lifecycleRetainsHistoryAndUpdatesAvailability() throws Exception {
        UUID borrowerId = createBorrower("alice@example.com");
        UUID bookId = createBook();

        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(bookId, borrowerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.returnedAt").doesNotExist());
        mvc.perform(get("/api/v1/books"))
                .andExpect(jsonPath("$[?(@.id == '%s')].available".formatted(bookId)).value(false));
        mvc.perform(post("/api/v1/books/{id}/return", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").exists());
        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(bookId, borrowerId)))
                .andExpect(status().isCreated());

        List<Loan> history = loanRepository.findAllByBookCopyIdOrderByBorrowedAt(bookId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getReturnedAt()).isNotNull();
        assertThat(history.get(1).getReturnedAt()).isNull();
    }

    @Test
    void listsBorrowersWithTheirCompleteLoanHistory() throws Exception {
        UUID borrowerId = createBorrower("history@example.com");
        UUID borrowerWithoutLoansId = createBorrower("no-history@example.com");
        UUID bookId = createBook();

        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(bookId, borrowerId)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/books/{id}/return", bookId))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(bookId, borrowerId)))
                .andExpect(status().isCreated());

        String body = mvc.perform(get("/api/v1/borrowers"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode borrowers = objectMapper.readTree(body);
        JsonNode borrower = findBorrower(borrowers, borrowerId);
        JsonNode borrowerWithoutLoans = findBorrower(borrowers, borrowerWithoutLoansId);

        assertThat(borrower.get("name").asText()).isEqualTo("Reader");
        assertThat(borrower.get("email").asText()).isEqualTo("history@example.com");
        assertThat(borrower.get("borrowHistory")).hasSize(2);
        assertThat(borrower.get("borrowHistory").get(0).get("bookId").asText()).isEqualTo(bookId.toString());
        assertThat(borrower.get("borrowHistory").get(0).get("returnedAt")).isNotNull();
        assertThat(borrower.get("borrowHistory").get(1).get("bookId").asText()).isEqualTo(bookId.toString());
        assertThat(borrower.get("borrowHistory").get(1).get("returnedAt").isNull()).isTrue();
        assertThat(borrowerWithoutLoans.get("borrowHistory")).isEmpty();
    }

    @Test
    void missingResourcesAndRepeatOperationsReturnExpectedErrors() throws Exception {
        UUID borrowerId = createBorrower("bob@example.com");
        UUID bookId = createBook();

        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(UUID.randomUUID(), borrowerId)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(loanJson(bookId, UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/books/{id}/return", bookId)).andExpect(status().isConflict());

        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(bookId, borrowerId))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(bookId, borrowerId))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/books/{id}/return", bookId)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/books/{id}/return", bookId)).andExpect(status().isConflict());
    }

    @Test
    void concurrentBorrowingCreatesExactlyOneActiveLoan() throws Exception {
        UUID firstBorrower = createBorrower("first@example.com");
        UUID secondBorrower = createBorrower("second@example.com");
        UUID bookId = createBook();
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> borrowAfter(start, bookId, firstBorrower));
            Future<Integer> second = executor.submit(() -> borrowAfter(start, bookId, secondBorrower));
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(loanRepository.findByBookCopyIdAndReturnedAtIsNull(bookId)).isPresent();
        assertThat(loanRepository.findAllByBookCopyIdOrderByBorrowedAt(bookId)).hasSize(1);
    }

    private int borrowAfter(CountDownLatch start, UUID bookId, UUID borrowerId) throws Exception {
        start.await();
        return mvc.perform(post("/api/v1/loans").contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(bookId, borrowerId))).andReturn().getResponse().getStatus();
    }

    private UUID createBorrower(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/borrowers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reader\",\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return id(body);
    }

    private UUID createBook() throws Exception {
        String body = mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9780306406157\",\"title\":\"Title\",\"author\":\"Author\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return id(body);
    }

    private UUID id(String body) throws Exception {
        JsonNode json = objectMapper.readTree(body);
        return UUID.fromString(json.get("id").asText());
    }

    private JsonNode findBorrower(JsonNode borrowers, UUID borrowerId) {
        for (JsonNode borrower : borrowers) {
            if (borrowerId.toString().equals(borrower.get("id").asText())) {
                return borrower;
            }
        }
        throw new AssertionError("Borrower not found: " + borrowerId);
    }

    private String loanJson(UUID bookId, UUID borrowerId) {
        return "{\"bookId\":\"%s\",\"borrowerId\":\"%s\"}".formatted(bookId, borrowerId);
    }
}
