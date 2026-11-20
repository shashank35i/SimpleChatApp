package com.chatapp.exception;

public class RoomAlreadyExistsException extends RuntimeException {
    public RoomAlreadyExistsException(String id) {
        super("Chat room '" + id + "' already exists.");
    }
}
