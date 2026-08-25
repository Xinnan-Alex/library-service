package com.alexleong.libraryservice.book;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BookCopyRepository extends JpaRepository<BookCopy, UUID> {
    @Query("select c from BookCopy c join fetch c.bookRecord order by c.createdAt, c.id")
    List<BookCopy> findAllWithBookRecord();

    @Query(value = "select book_copy_id from loans where returned_at is null", nativeQuery = true)
    Set<UUID> findActiveLoanBookCopyIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.id = :id")
    Optional<BookCopy> findByIdForUpdate(UUID id);
}