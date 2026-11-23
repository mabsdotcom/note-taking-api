package com.notesapp.exception;

/** 404 - also used when a resource exists but belongs to another user, so ids are not leaked. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
