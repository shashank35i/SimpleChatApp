# Simple Chat Application

Spring Boot backend using Redis for rooms, history and live messages. Requires Java 17.

Deployed backend: https://freightfox-simpleapp-production.up.railway.app

Swagger UI: https://freightfox-simpleapp-production.up.railway.app/swagger-ui.html

```text
HTTP -> controller -> service -> repository -> Redis
          SSE registry <- subscriber <- Redis Pub/Sub
```

DTO records define requests and responses. Constructor injection connects the components. The repository stores room data, while the publisher sends events using StringRedisTemplate. RedisKeys centralizes key names. The subscriber uses the Observer pattern to notify listeners.

| Key | Type | Contents |
| --- | --- | --- |
| `chatroom:{roomId}:meta` | hash | roomName and createdAt |
| `chatroom:{roomId}:participants` | set | participants |
| `chatroom:{roomId}:messages` | list | JSON messages |
| `chatroom:{roomId}` | channel | JSON messages |

HSETNX gates room creation atomically. History is trimmed to 1000 messages by default; set `CHAT_MAX_MESSAGES_PER_ROOM` to change it. Sending requires membership. Pub/Sub distributes messages to app instances and SSE delivers them to clients. Pub/Sub is fire and forget, so reconnecting clients should fetch history. There is no authentication. Concurrent deletion and other room operations are not transactional.

Start Redis locally, then run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). Set `REDIS_HOST`, `REDIS_PORT` and `SERVER_PORT` as needed. Alternatively run `docker compose up --build -d`. Redis uses a named volume. Health is at `/actuator/health`, Swagger UI at `/swagger-ui.html`.

```sh
curl -X POST localhost:8080/api/chatapp/chatrooms -H 'Content-Type: application/json' -d '{"roomName":"general"}'
curl -X POST localhost:8080/api/chatapp/chatrooms/general/join -H 'Content-Type: application/json' -d '{"participant":"guest_user"}'
curl -N localhost:8080/api/chatapp/chatrooms/general/stream
```

In another terminal:

```sh
curl -X POST localhost:8080/api/chatapp/chatrooms/general/messages -H 'Content-Type: application/json' -d '{"participant":"guest_user","message":"Hello, everyone!"}'
curl 'localhost:8080/api/chatapp/chatrooms/general/messages?limit=10'
curl -X DELETE localhost:8080/api/chatapp/chatrooms/general
```

Run tests with `mvn clean verify` or `./mvnw clean verify` (Windows: `mvnw.cmd clean verify`). Docker is required for Testcontainers tests. JaCoCo writes `target/site/jacoco/index.html`.
