package com.chatapp.messaging;

import com.chatapp.dto.ChatMessage;

public interface MessageListener {
    void onMessage(String roomId, ChatMessage message);
}
