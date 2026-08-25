package com.alexleong.libraryservice.borrower;

import com.alexleong.libraryservice.book.BookCopy;
import com.alexleong.libraryservice.loan.Loan;
import com.alexleong.libraryservice.loan.LoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowerServiceTest {
    @Test
    void trimsAndCreatesBorrower() {
        BorrowerRepository repository = mock(BorrowerRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerResponse response = new BorrowerService(repository, mock(LoanRepository.class))
                .create(new CreateBorrowerRequest("  Alice  ", " alice@example.com "));

        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void listsBorrowersWithCompleteAndEmptyHistories() {
        BorrowerRepository borrowerRepository = mock(BorrowerRepository.class);
        LoanRepository loanRepository = mock(LoanRepository.class);
        Borrower borrower = borrower(UUID.randomUUID(), "Alice", "alice@example.com");
        Borrower borrowerWithoutLoans = borrower(UUID.randomUUID(), "Bob", "bob@example.com");
        Loan loan = loan(UUID.randomUUID(), UUID.randomUUID(), borrower, Instant.parse("2026-08-25T10:00:00Z"));
        when(borrowerRepository.findAll(any(Sort.class))).thenReturn(List.of(borrower, borrowerWithoutLoans));
        when(loanRepository.findAllWithBorrowerAndBookCopyOrderByBorrowedAt()).thenReturn(List.of(loan));

        List<BorrowerHistoryResponse> response = new BorrowerService(borrowerRepository, loanRepository).list();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).borrowHistory()).hasSize(1);
        assertThat(response.get(0).borrowHistory().get(0).borrowerId()).isEqualTo(borrower.getId());
        assertThat(response.get(1).borrowHistory()).isEmpty();
    }

    private Borrower borrower(UUID id, String name, String email) {
        Borrower borrower = mock(Borrower.class);
        when(borrower.getId()).thenReturn(id);
        when(borrower.getName()).thenReturn(name);
        when(borrower.getEmail()).thenReturn(email);
        return borrower;
    }

    private Loan loan(UUID id, UUID bookId, Borrower borrower, Instant borrowedAt) {
        BookCopy bookCopy = mock(BookCopy.class);
        when(bookCopy.getId()).thenReturn(bookId);
        Loan loan = mock(Loan.class);
        when(loan.getId()).thenReturn(id);
        when(loan.getBookCopy()).thenReturn(bookCopy);
        when(loan.getBorrower()).thenReturn(borrower);
        when(loan.getBorrowedAt()).thenReturn(borrowedAt);
        return loan;
    }
}