package com.alexleong.libraryservice.loan;

import com.alexleong.libraryservice.book.BookCopy;
import com.alexleong.libraryservice.book.BookCopyRepository;
import com.alexleong.libraryservice.borrower.Borrower;
import com.alexleong.libraryservice.borrower.BorrowerRepository;
import com.alexleong.libraryservice.error.ConflictException;
import com.alexleong.libraryservice.error.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowerRepository borrowerRepository;
    private final Clock clock;

    @Autowired
    public LoanService(LoanRepository loanRepository, BookCopyRepository bookCopyRepository,
                       BorrowerRepository borrowerRepository) {
        this(loanRepository, bookCopyRepository, borrowerRepository, Clock.systemUTC());
    }

    LoanService(LoanRepository loanRepository, BookCopyRepository bookCopyRepository,
                BorrowerRepository borrowerRepository, Clock clock) {
        this.loanRepository = loanRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowerRepository = borrowerRepository;
        this.clock = clock;
    }

    @Transactional
    public LoanResponse borrow(CreateLoanRequest request) {
        BookCopy copy = lockedCopy(request.bookId());
        Borrower borrower = borrowerRepository.findById(request.borrowerId())
                .orElseThrow(() -> new NotFoundException("Borrower not found"));
        if (loanRepository.findByBookCopyIdAndReturnedAtIsNull(copy.getId()).isPresent()) {
            throw new ConflictException("Book is already borrowed");
        }

        try {
            // Flush immediately to catch database constraint violations in this try-catch block
            return response(loanRepository.saveAndFlush(new Loan(copy, borrower, Instant.now(clock))));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Book is already borrowed");
        }
    }

    @Transactional
    public LoanResponse returnBook(UUID bookId) {
        BookCopy copy = lockedCopy(bookId);
        Loan loan = loanRepository.findByBookCopyIdAndReturnedAtIsNull(copy.getId())
                .orElseThrow(() -> new ConflictException("Book is not currently borrowed"));
        loan.returnAt(Instant.now(clock));
        return response(loan);
    }

    private BookCopy lockedCopy(UUID bookId) {
        return bookCopyRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
    }

    private LoanResponse response(Loan loan) {
        return LoanResponse.from(loan);
    }
}
