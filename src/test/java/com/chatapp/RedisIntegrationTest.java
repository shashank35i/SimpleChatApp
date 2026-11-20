package com.chatapp;

import static org.junit.jupiter.api.Assertions.*;

import com.chatapp.dto.ChatMessage;
import com.chatapp.exception.RoomAlreadyExistsException;
import com.chatapp.messaging.MessageListener;
import com.chatapp.repository.RedisKeys;
import com.chatapp.service.ChatRoomService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RedisIntegrationTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @TestConfiguration
    static class ListenerConfig {
        @Bean
        Capture capture() {
            return new Capture();
        }
    }

    static class Capture implements MessageListener {
        final BlockingQueue<ChatMessage> messages = new LinkedBlockingQueue<>();

        public void onMessage(String roomId, ChatMessage message) {
            if (roomId.equals("integration")) messages.add(message);
        }
    }

    @Autowired ChatRoomService service;
    @Autowired StringRedisTemplate template;
    @Autowired Capture capture;

    @BeforeEach
    void clear() {
        try (var connection = template.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
        capture.messages.clear();
    }

    @Test
    void createJoinSendHistoryAndDelete() throws Exception {
        service.create("integration");
        service.join("integration", "guest");
        assertEquals(
                "integration",
                template.opsForHash().get(RedisKeys.meta("integration"), "roomName"));
        assertTrue(template.opsForSet().isMember(RedisKeys.participants("integration"), "guest"));
        service.send("integration", "guest", "one");
        service.send("integration", "guest", "two");
        assertEquals(
                java.util.List.of("one", "two"),
                service.history("integration", 10).stream().map(ChatMessage::message).toList());
        assertEquals("two", service.history("integration", 1).get(0).message());
        assertEquals("one", capture.messages.poll(5, TimeUnit.SECONDS).message());
        service.delete("integration");
        assertFalse(template.hasKey(RedisKeys.meta("integration")));
        assertFalse(template.hasKey(RedisKeys.participants("integration")));
        assertFalse(template.hasKey(RedisKeys.messages("integration")));
    }

    @Test
    void concurrentCreate_hasOneWinner() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        java.util.List<Future<?>> tasks = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < 50; i++)
                tasks.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    try {
                                        service.create("race");
                                        wins.incrementAndGet();
                                    } catch (RoomAlreadyExistsException expected) {
                                    }
                                    return null;
                                }));
            start.countDown();
            for (Future<?> task : tasks) task.get(30, TimeUnit.SECONDS);
            assertEquals(1, wins.get());
        } finally {
            pool.shutdownNow();
        }
    }
}
