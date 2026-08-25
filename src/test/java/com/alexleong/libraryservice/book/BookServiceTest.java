package com.alexleong.libraryservice.book;

import com.alexleong.libraryservice.error.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock private BookRecordRepository recordRepository;
    @Mock private BookCopyRepository copyRepository;
    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(recordRepository, copyRepository);
    }

    @Test
    void createsMultiplePhysicalCopiesForSameNormalizedIsbn() {
        BookRecord record = new BookRecord("9780306406157", "The Book", "The Author");
        when(copyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.findByIsbn("9780306406157"))
                .thenReturn(Optional.empty(), Optional.of(record));
        when(recordRepository.save(any())).thenReturn(record);

        BookResponse first = service.create(new CreateBookRequest("978-0-306-40615-7", " The Book ", "The Author"));
        BookResponse second = service.create(new CreateBookRequest("9780306406157", "The Book", "The Author"));

        assertThat(first.isbn()).isEqualTo("9780306406157");
        assertThat(second.title()).isEqualTo("The Book");
        verify(copyRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void rejectsDifferentMetadataForExistingIsbn() {
        when(recordRepository.findByIsbn("9780306406157"))
                .thenReturn(Optional.of(new BookRecord("9780306406157", "Original", "Author")));

        assertThatThrownBy(() -> service.create(new CreateBookRequest("9780306406157", "Different", "Author")))
                .isInstanceOf(ConflictException.class);
        verify(copyRepository, never()).save(any());
    }

    @Test
    void listsEveryCopyAndCurrentAvailability() throws Exception {
        BookRecord record = new BookRecord("9780306406157", "Title", "Author");
        BookCopy available = copyWithId(record, UUID.randomUUID());
        BookCopy unavailable = copyWithId(record, UUID.randomUUID());
        when(copyRepository.findActiveLoanBookCopyIds()).thenReturn(Set.of(unavailable.getId()));
        when(copyRepository.findAllWithBookRecord()).thenReturn(List.of(available, unavailable));

        assertThat(service.list()).extracting(BookResponse::available).containsExactly(true, false);
    }

    private BookCopy copyWithId(BookRecord record, UUID id) throws Exception {
        BookCopy copy = new BookCopy(record);
        Field field = BookCopy.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(copy, id);
        return copy;
    }
}