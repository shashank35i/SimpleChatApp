package com.chatapp.messaging;

import com.chatapp.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MessageSubscriber
        implements org.springframework.data.redis.connection.MessageListener {
    private static final Logger log = LoggerFactory.getLogger(MessageSubscriber.class);
    private final ObjectMapper mapper;
    private final List<MessageListener> listeners;
    private final StringRedisTemplate redis;
    private final ExecutorService messageExecutor = Executors.newSingleThreadExecutor();
    private RedisMessageListenerContainer container;

    public MessageSubscriber(
            ObjectMapper mapper, List<MessageListener> listeners, StringRedisTemplate redis) {
        this.mapper = mapper;
        this.listeners = listeners;
        this.redis = redis;
    }

    @PostConstruct
    void subscribe() {
        container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redis.getConnectionFactory());
        container.setTaskExecutor(messageExecutor);
        container.setSubscriptionExecutor(new SimpleAsyncTaskExecutor("redis-subscription-"));
        container.addMessageListener(this, new PatternTopic("chatroom:*"));
        container.afterPropertiesSet();
        container.start();
    }

    @PreDestroy
    void close() throws Exception {
        try {
            if (container != null) {
                container.destroy();
            }
        } finally {
            messageExecutor.shutdown();
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String roomId = channel.substring("chatroom:".length());
        if (roomId.contains(":")) {
            return;
        }
        try {
            ChatMessage chat = mapper.readValue(message.getBody(), ChatMessage.class);
            for (MessageListener listener : listeners) {
                try {
                    listener.onMessage(roomId, chat);
                } catch (RuntimeException e) {
                    log.warn("Message listener failed", e);
                }
            }
        } catch (Exception e) {
            log.warn("Invalid chat message", e);
        }
    }
}
