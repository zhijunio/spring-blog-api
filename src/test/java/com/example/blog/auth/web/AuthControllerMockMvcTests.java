package com.example.blog.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.AbstractIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class AuthControllerMockMvcTests extends AbstractIT {
    @Test
    @DisplayName("Given valid credentials, user should be able to login successfully")
    void shouldLoginSuccessfully() {
        MvcTestResult testResult = mvc.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email":"user@gmail.com",
                            "password":"siva"
                        }
                        """)
                .exchange();

        assertThat(testResult)
                .hasStatusOk()
                .bodyJson()
                .convertTo(LoginResponse.class)
                .satisfies(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.name()).isEqualTo("Siva Prasad");
                    assertThat(response.email()).isEqualTo("user@gmail.com");
                    assertThat(response.token()).isNotBlank();
                });
    }
}
