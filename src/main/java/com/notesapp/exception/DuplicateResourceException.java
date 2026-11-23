package com.notesapp.exception;

/** 409 - a unique value (username, email, category or tag name) is already taken. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
