package com.notesapp.exception;

/** 401 - refresh token unknown, expired or revoked. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
