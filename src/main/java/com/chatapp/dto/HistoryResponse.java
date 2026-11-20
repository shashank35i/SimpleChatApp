package com.chatapp.dto;

import java.util.List;

public record HistoryResponse(List<ChatMessage> messages) {}
