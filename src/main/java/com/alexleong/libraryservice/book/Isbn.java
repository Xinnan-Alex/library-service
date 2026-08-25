package com.alexleong.libraryservice.book;

import com.alexleong.libraryservice.error.InvalidRequestException;

import java.util.Locale;

public final class Isbn {
    private Isbn() { }

    public static String normalize(String value) {
        String isbn = value.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
        if (!isValid10(isbn) && !isValid13(isbn)) {
            throw new InvalidRequestException("isbn", "must be a valid ISBN-10 or ISBN-13");
        }
        return isbn;
    }

    private static boolean isValid10(String isbn) {
        if (!isbn.matches("[0-9]{9}[0-9X]")) return false;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int digit = isbn.charAt(i) == 'X' ? 10 : isbn.charAt(i) - '0';
            sum += (10 - i) * digit;
        }
        return sum % 11 == 0;
    }

    private static boolean isValid13(String isbn) {
        if (!isbn.matches("[0-9]{13}")) return false;
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (isbn.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return (10 - sum % 10) % 10 == isbn.charAt(12) - '0';
    }
}