package com.chatapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chatapp.exception.*;
import com.chatapp.messaging.MessagePublisher;
import com.chatapp.repository.ChatRoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

class ChatRoomServiceTest {
    ChatRoomRepository repository = mock(ChatRoomRepository.class);
    MessagePublisher publisher = mock(MessagePublisher.class);
    ChatRoomService service =
            new ChatRoomService(
                    repository, publisher, new ObjectMapper().findAndRegisterModules(), 1000);

    @Test
    void createRoom_succeeds() {
        when(repository.create("general")).thenReturn(true);
        service.create("general");
        verify(repository).create("general");
    }

    @Test
    void createRoom_throwsWhenDuplicate() {
        assertThrows(RoomAlreadyExistsException.class, () -> service.create("general"));
    }

    @Test
    void joinRoom_addsParticipant() {
        when(repository.exists("general")).thenReturn(true);
        when(repository.join("general", "guest")).thenReturn(true);
        assertTrue(service.join("general", "guest"));
    }

    @Test
    void joinRoom_isIdempotent() {
        when(repository.exists("general")).thenReturn(true);
        assertFalse(service.join("general", "guest"));
    }

    @Test
    void joinRoom_throwsWhenRoomMissing() {
        assertThrows(RoomNotFoundException.class, () -> service.join("general", "guest"));
    }

    @Test
    void sendMessage_savesAndPublishes() {
        when(repository.exists("general")).thenReturn(true);
        when(repository.isMember("general", "guest")).thenReturn(true);
        service.send("general", "guest", "hello");
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(repository).append(eq("general"), json.capture(), eq(1000));
        verify(publisher).publish("general", json.getValue());
    }

    @Test
    void sendMessage_throwsWhenRoomMissing() {
        assertThrows(RoomNotFoundException.class, () -> service.send("general", "guest", "hello"));
    }

    @Test
    void sendMessage_throwsWhenNotMember() {
        when(repository.exists("general")).thenReturn(true);
        assertThrows(
                ParticipantNotInRoomException.class,
                () -> service.send("general", "guest", "hello"));
    }

    @Test
    void history_preservesOrderAndLimit() {
        when(repository.exists("general")).thenReturn(true);
        when(repository.history("general", 2))
                .thenReturn(
                        List.of(
                                "{\"participant\":\"a\",\"message\":\"one\",\"timestamp\":\"2024-01-01T10:00:00Z\"}",
                                "{\"participant\":\"a\",\"message\":\"two\",\"timestamp\":\"2024-01-01T10:01:00Z\"}"));
        assertEquals(
                List.of("one", "two"),
                service.history("general", 2).stream().map(m -> m.message()).toList());
    }

    @Test
    void deleteRoom_removesKeys() {
        when(repository.exists("general")).thenReturn(true);
        service.delete("general");
        verify(repository).delete("general");
    }
}
