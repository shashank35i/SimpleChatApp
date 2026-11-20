package com.chatapp;

import com.chatapp.messaging.MessageSubscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {
    @Autowired
    MockMvc mvc;

    @MockBean
    MessageSubscriber subscriber;

    @Test
    void apiDocs_useCurrentOrigin() throws Exception {
        mvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "chat.example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("/"));
    }
}
