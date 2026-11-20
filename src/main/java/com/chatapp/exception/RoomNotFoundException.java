package com.chatapp.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String id) {
        super("Chat room '" + id + "' does not exist.");
    }
}
