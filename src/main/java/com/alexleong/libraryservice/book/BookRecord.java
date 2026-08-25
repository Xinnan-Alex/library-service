package com.alexleong.libraryservice.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "book_records")
public class BookRecord {
    @Id @UuidGenerator private UUID id;
    @Column(nullable = false, unique = true, length = 13) private String isbn;
    @Column(nullable = false, length = 500) private String title;
    @Column(nullable = false, length = 300) private String author;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected BookRecord() { }
    BookRecord(String isbn, String title, String author) {
        this.isbn = isbn; this.title = title; this.author = author;
    }
    public UUID getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
}