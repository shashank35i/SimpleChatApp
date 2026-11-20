package com.chatapp.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chatapp.exception.*;
import com.chatapp.messaging.SseEmitterRegistry;
import com.chatapp.service.ChatRoomService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ChatRoomService service;
    @MockBean SseEmitterRegistry registry;
    String base = "/api/chatapp/chatrooms";

    @Test
    void create_returnsCreated() throws Exception {
        mvc.perform(
                        post(base)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roomName\":\"general\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value("general"));
    }

    @Test
    void create_rejectsInvalidAndMalformed() throws Exception {
        mvc.perform(
                        post(base)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roomName\":\"bad name\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
        mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsConflict() throws Exception {
        doThrow(new RoomAlreadyExistsException("general")).when(service).create("general");
        mvc.perform(
                        post(base)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roomName\":\"general\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void join_reportsBothOutcomes() throws Exception {
        when(service.join("general", "guest")).thenReturn(true, false);
        var request =
                post(base + "/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"guest\"}");
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User 'guest' joined chat room 'general'."));
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "User 'guest' is already a participant in chat room"
                                            + " 'general'."));
    }

    @Test
    void send_returnsSuccessAndForbidden() throws Exception {
        var request =
                post(base + "/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"guest\",\"message\":\"hello\"}");
        mvc.perform(request).andExpect(status().isOk());
        doThrow(new ParticipantNotInRoomException("guest", "general"))
                .when(service)
                .send("general", "guest", "hello");
        mvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    void send_rejectsBlank() throws Exception {
        mvc.perform(
                        post(base + "/general/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"participant\":\"guest\",\"message\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void history_rejectsInvalidLimits() throws Exception {
        for (String limit : new String[] {"0", "101", "abc"})
            mvc.perform(get(base + "/general/messages").param("limit", limit))
                    .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returnsSuccess() throws Exception {
        mvc.perform(delete(base + "/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void stream_startsAsync() throws Exception {
        when(registry.subscribe("general")).thenReturn(new SseEmitter());
        mvc.perform(get(base + "/general/stream"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void missingRoom_returns404() throws Exception {
        doThrow(new RoomNotFoundException("missing")).when(service).requireRoom("missing");
        mvc.perform(get(base + "/missing/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void unexpectedError_doesNotExposeDetails() throws Exception {
        doThrow(new IllegalStateException("secret")).when(service).create("general");
        mvc.perform(
                        post(base)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roomName\":\"general\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }
}
