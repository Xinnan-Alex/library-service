package com.alexleong.libraryservice.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "book_copies")
public class BookCopy {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_record_id", nullable = false)
    private BookRecord bookRecord;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected BookCopy() { }
    BookCopy(BookRecord bookRecord) { this.bookRecord = bookRecord; }
    public UUID getId() { return id; }
    public BookRecord getBookRecord() { return bookRecord; }
}