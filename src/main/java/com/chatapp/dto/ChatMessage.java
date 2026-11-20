package com.chatapp.dto;

import java.time.Instant;

public record ChatMessage(String participant, String message, Instant timestamp) {}
