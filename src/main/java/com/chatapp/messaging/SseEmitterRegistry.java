package com.chatapp.messaging;

import com.chatapp.dto.ChatMessage;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry implements MessageListener {
    private final ConcurrentHashMap<String, Set<SseEmitter>> rooms = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String roomId) {
        SseEmitter emitter = new SseEmitter(0L);
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable remove = () -> remove(roomId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        return emitter;
    }

    private void remove(String roomId, SseEmitter emitter) {
        rooms.computeIfPresent(roomId, (key, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }

    @Override
    public void onMessage(String roomId, ChatMessage message) {
        Set<SseEmitter> emitters = rooms.get(roomId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e);
            }
        }
    }
}
