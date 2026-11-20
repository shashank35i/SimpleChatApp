package com.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(example = "{\"roomName\":\"general\"}")
public record CreateRoomRequest(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$") String roomName) {}
