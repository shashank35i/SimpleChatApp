package com.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(example = "{\"participant\":\"guest_user\",\"message\":\"Hello, everyone!\"}")
public record SendMessageRequest(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$") String participant,
        @NotBlank @Size(max = 2000) String message) {}
