package com.alexleong.libraryservice.loan;

import com.alexleong.libraryservice.book.BookCopy;
import com.alexleong.libraryservice.borrower.Borrower;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loans")
public class Loan {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy bookCopy;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;
    @Column(name = "borrowed_at", nullable = false, updatable = false) private Instant borrowedAt;
    @Column(name = "returned_at") private Instant returnedAt;

    protected Loan() { }

    Loan(BookCopy bookCopy, Borrower borrower, Instant borrowedAt) {
        this.bookCopy = bookCopy;
        this.borrower = borrower;
        this.borrowedAt = borrowedAt;
    }

    void returnAt(Instant returnedAt) { this.returnedAt = returnedAt; }

    public UUID getId() { return id; }
    public BookCopy getBookCopy() { return bookCopy; }
    public Borrower getBorrower() { return borrower; }
    public Instant getBorrowedAt() { return borrowedAt; }
    public Instant getReturnedAt() { return returnedAt; }
}
