package com.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(example = "{\"participant\":\"guest_user\"}")
public record JoinRequest(@NotNull @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$") String participant) {}
