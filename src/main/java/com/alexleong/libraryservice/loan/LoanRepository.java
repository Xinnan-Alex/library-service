package com.alexleong.libraryservice.loan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    Optional<Loan> findByBookCopyIdAndReturnedAtIsNull(UUID bookCopyId);
    List<Loan> findAllByBookCopyIdOrderByBorrowedAt(UUID bookCopyId);

    @Query("""
            select loan from Loan loan
            join fetch loan.bookCopy
            join fetch loan.borrower
            order by loan.borrowedAt, loan.id
            """)
    List<Loan> findAllWithBorrowerAndBookCopyOrderByBorrowedAt();
}
