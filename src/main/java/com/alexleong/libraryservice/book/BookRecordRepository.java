package com.alexleong.libraryservice.book;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookRecordRepository extends JpaRepository<BookRecord, UUID> {
    Optional<BookRecord> findByIsbn(String isbn);
}