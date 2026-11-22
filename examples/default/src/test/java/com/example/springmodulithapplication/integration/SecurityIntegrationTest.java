package com.example.springmodulithapplication.integration;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springmodulithapplication.support.AbstractPostgresIntegrationTest;
import com.example.springmodulithapplication.support.IntegrationTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@SpringBootTest(properties = {
        "app.modules.identity.enabled=true",
        "app.modules.notifications.enabled=false",
        "app.modules.audit.enabled=false",
        "app.integrations.rabbitmq.enabled=false",
        "app.integrations.stomp.enabled=false"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationIsPublicButProfileAccessRequiresAuthentication() throws Exception {
        String username = "security-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "correct-horse-battery";
        String body = """
                {"username":"%s","email":"%s@example.com","password":"%s"}
                """.formatted(username, username, password);

        mockMvc.perform(post("/api/identity/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(get("/api/identity/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/identity/me").with(httpBasic(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
