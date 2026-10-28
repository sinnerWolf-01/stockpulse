package com.stockpulse.suggestion;

public class SuggestionNotFoundException extends RuntimeException {
    public SuggestionNotFoundException(String type, Long id) {
        super(type + " suggestion not found: " + id);
    }
}