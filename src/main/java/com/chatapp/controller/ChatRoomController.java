package com.chatapp.controller;

import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.HistoryResponse;
import com.chatapp.dto.JoinRequest;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.dto.StatusResponse;
import com.chatapp.messaging.SseEmitterRegistry;
import com.chatapp.service.ChatRoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chatapp/chatrooms")
@Validated
public class ChatRoomController {
    private final ChatRoomService service;
    private final SseEmitterRegistry emitters;

    public ChatRoomController(ChatRoomService service, SseEmitterRegistry emitters) {
        this.service = service;
        this.emitters = emitters;
    }

    @Operation(summary = "Create a room")
    @ApiResponse(responseCode = "201", description = "Room created")
    @PostMapping
    public ResponseEntity<StatusResponse> create(
            @Valid @RequestBody CreateRoomRequest request) {
        service.create(request.roomName());
        String message = "Chat room '" + request.roomName() + "' created successfully.";
        return ResponseEntity.status(201).body(new StatusResponse(message, request.roomName(), "success"));
    }

    @Operation(summary = "Join a room")
    @ApiResponse(responseCode = "200", description = "Participant joined")
    @PostMapping("/{roomId}/join")
    public StatusResponse join(
            @PathVariable String roomId,
            @Valid @RequestBody JoinRequest request) {
        boolean joined = service.join(roomId, request.participant());
        String message;
        if (joined) {
            message = "User '" + request.participant() + "' joined chat room '" + roomId + "'.";
        } else {
            message = "User '" + request.participant() + "' is already a participant in chat room '" + roomId + "'.";
        }
        return new StatusResponse(message, null, "success");
    }

    @Operation(summary = "Send a message")
    @ApiResponse(responseCode = "200", description = "Message sent")
    @PostMapping("/{roomId}/messages")
    public StatusResponse send(
            @PathVariable String roomId,
            @Valid @RequestBody SendMessageRequest request) {
        service.send(roomId, request.participant(), request.message());
        return new StatusResponse("Message sent successfully.", null, "success");
    }

    @Operation(summary = "Get message history")
    @ApiResponse(responseCode = "200", description = "Messages in chronological order")
    @GetMapping("/{roomId}/messages")
    public HistoryResponse history(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return new HistoryResponse(service.history(roomId, limit));
    }

    @Operation(summary = "Delete a room")
    @ApiResponse(responseCode = "200", description = "Room deleted")
    @DeleteMapping("/{roomId}")
    public StatusResponse delete(@PathVariable String roomId) {
        service.delete(roomId);
        return new StatusResponse(
                "Chat room '" + roomId + "' deleted successfully.", null, "success");
    }

    @Operation(summary = "Stream new messages")
    @ApiResponse(responseCode = "200", description = "Event stream")
    @GetMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String roomId) {
        service.requireRoom(roomId);
        return emitters.subscribe(roomId);
    }
}
