package com.alexleong.libraryservice.book;

import com.alexleong.libraryservice.error.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsbnTest {
    @Test
    void normalizesValidIsbn10And13() {
        assertThat(Isbn.normalize("0-306-40615-2")).isEqualTo("0306406152");
        assertThat(Isbn.normalize("978 0 306 40615 7")).isEqualTo("9780306406157");
    }

    @Test
    void rejectsInvalidChecksum() {
        assertThatThrownBy(() -> Isbn.normalize("9780306406158"))
                .isInstanceOf(InvalidRequestException.class);
    }
}