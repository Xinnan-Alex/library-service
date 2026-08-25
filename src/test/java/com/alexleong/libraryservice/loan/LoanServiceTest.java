package com.alexleong.libraryservice.loan;

import com.alexleong.libraryservice.book.BookCopy;
import com.alexleong.libraryservice.book.BookCopyRepository;
import com.alexleong.libraryservice.borrower.Borrower;
import com.alexleong.libraryservice.borrower.BorrowerRepository;
import com.alexleong.libraryservice.error.ConflictException;
import com.alexleong.libraryservice.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {
    @Mock private LoanRepository loanRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private BorrowerRepository borrowerRepository;
    @Mock private BookCopy copy;
    @Mock private Borrower borrower;
    private LoanService service;
    private UUID bookId;
    private UUID borrowerId;

    @BeforeEach
    void setUp() {
        Instant now = Instant.parse("2026-08-21T12:00:00Z");
        service = new LoanService(loanRepository, bookCopyRepository, borrowerRepository,
                Clock.fixed(now, ZoneOffset.UTC));
        bookId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();
        when(copy.getId()).thenReturn(bookId);
    }

    @Test
    void borrowsAndReturnsWhileKeepingTheOriginalLoan() {
        when(borrower.getId()).thenReturn(borrowerId);
        when(bookCopyRepository.findByIdForUpdate(bookId)).thenReturn(Optional.of(copy));
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(loanRepository.findByBookCopyIdAndReturnedAtIsNull(bookId)).thenReturn(Optional.empty());
        when(loanRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponse borrowed = service.borrow(new CreateLoanRequest(bookId, borrowerId));
        Loan loan = new Loan(copy, borrower, borrowed.borrowedAt());
        when(loanRepository.findByBookCopyIdAndReturnedAtIsNull(bookId)).thenReturn(Optional.of(loan));

        LoanResponse returned = service.returnBook(bookId);

        assertThat(borrowed.borrowedAt()).isEqualTo(Instant.parse("2026-08-21T12:00:00Z"));
        assertThat(returned.returnedAt()).isEqualTo(borrowed.borrowedAt());
    }

    @Test
    void reportsMissingReferencesAndInvalidState() {
        when(bookCopyRepository.findByIdForUpdate(bookId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.borrow(new CreateLoanRequest(bookId, borrowerId)))
                .isInstanceOf(NotFoundException.class);

        when(bookCopyRepository.findByIdForUpdate(bookId)).thenReturn(Optional.of(copy));
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.borrow(new CreateLoanRequest(bookId, borrowerId)))
                .isInstanceOf(NotFoundException.class);

        when(loanRepository.findByBookCopyIdAndReturnedAtIsNull(bookId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.returnBook(bookId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void translatesDefensiveUniqueConstraintViolation() {
        when(bookCopyRepository.findByIdForUpdate(bookId)).thenReturn(Optional.of(copy));
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(loanRepository.findByBookCopyIdAndReturnedAtIsNull(bookId)).thenReturn(Optional.empty());
        when(loanRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique index"));

        assertThatThrownBy(() -> service.borrow(new CreateLoanRequest(bookId, borrowerId)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Book is already borrowed");
    }
}
