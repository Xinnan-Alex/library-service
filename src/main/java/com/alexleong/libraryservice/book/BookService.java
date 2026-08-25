package com.alexleong.libraryservice.book;

import com.alexleong.libraryservice.error.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BookService {
    private final BookRecordRepository recordRepository;
    private final BookCopyRepository copyRepository;

    public BookService(BookRecordRepository recordRepository, BookCopyRepository copyRepository) {
        this.recordRepository = recordRepository;
        this.copyRepository = copyRepository;
    }

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        String isbn = Isbn.normalize(request.isbn());
        String title = request.title().trim();
        String author = request.author().trim();
        BookRecord record = recordRepository.findByIsbn(isbn).map(existing -> {
            if (!existing.getTitle().equals(title) || !existing.getAuthor().equals(author)) {
                throw new ConflictException("ISBN is already registered with different title or author");
            }
            return existing;
        }).orElseGet(() -> recordRepository.save(new BookRecord(isbn, title, author)));
        return BookResponse.from(copyRepository.save(new BookCopy(record)), true);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> list() {
        Set<UUID> unavailableCopyIds = copyRepository.findActiveLoanBookCopyIds();
        return copyRepository.findAllWithBookRecord().stream()
                .map(copy -> BookResponse.from(copy, !unavailableCopyIds.contains(copy.getId())))
                .toList();
    }
}