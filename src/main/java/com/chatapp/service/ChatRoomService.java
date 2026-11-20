package com.chatapp.service;

import com.chatapp.dto.ChatMessage;
import com.chatapp.exception.ParticipantNotInRoomException;
import com.chatapp.exception.RoomAlreadyExistsException;
import com.chatapp.exception.RoomNotFoundException;
import com.chatapp.messaging.MessagePublisher;
import com.chatapp.repository.ChatRoomRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChatRoomService {
    private static final Logger log = LoggerFactory.getLogger(ChatRoomService.class);
    private final ChatRoomRepository repository;
    private final MessagePublisher publisher;
    private final ObjectMapper mapper;
    private final int maxMessages;

    public ChatRoomService(
            ChatRoomRepository repository,
            MessagePublisher publisher,
            ObjectMapper mapper,
            @Value("${chat.max-messages-per-room:1000}") int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("max messages must be positive");
        }
        this.repository = repository;
        this.publisher = publisher;
        this.mapper = mapper;
        this.maxMessages = maxMessages;
    }

    public void requireRoom(String id) {
        if (!repository.exists(id)) {
            throw new RoomNotFoundException(id);
        }
    }

    public void create(String id) {
        if (!repository.create(id)) {
            throw new RoomAlreadyExistsException(id);
        }
        log.info("Created room {}", id);
    }

    public boolean join(String id, String participant) {
        requireRoom(id);
        return repository.join(id, participant);
    }

    public void send(String id, String participant, String message) {
        requireRoom(id);
        if (!repository.isMember(id, participant)) {
            throw new ParticipantNotInRoomException(participant, id);
        }
        try {
            String json =
                    mapper.writeValueAsString(new ChatMessage(participant, message, Instant.now()));
            repository.append(id, json, maxMessages);
            publisher.publish(id, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize message", e);
        }
    }

    public List<ChatMessage> history(String id, int limit) {
        requireRoom(id);
        return repository.history(id, limit).stream().map(this::readMessage).toList();
    }

    private ChatMessage readMessage(String json) {
        try {
            return mapper.readValue(json, ChatMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read message", e);
        }
    }

    public void delete(String id) {
        requireRoom(id);
        repository.delete(id);
        log.info("Deleted room {}", id);
    }
}
