package com.chatapp.exception;

public class ParticipantNotInRoomException extends RuntimeException {
    public ParticipantNotInRoomException(String user, String id) {
        super("User '" + user + "' is not a participant in chat room '" + id + "'.");
    }
}
