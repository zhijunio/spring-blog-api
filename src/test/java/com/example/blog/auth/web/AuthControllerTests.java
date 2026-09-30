package com.example.blog.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthControllerTests extends AbstractIT {

    @Test
    void shouldLoginSuccessfully() {
        LoginResponse response = restTestClient
                .post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "email":"user@gmail.com",
                            "password":"siva"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(LoginResponse.class)
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Siva Prasad");
        assertThat(response.email()).isEqualTo("user@gmail.com");
        assertThat(response.token()).isNotBlank();
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() {
        String response = restTestClient
                .post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "email":"user@gmail.com",
                          "password":"invalid-password"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .returnResult(String.class)
                .getResponseBody();

        assertThat(response).contains("Unauthorized");
    }

    @Test
    void shouldReturnValidationErrorsForInvalidLoginRequest() {
        String response = restTestClient
                .post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "email":"invalid-email",
                          "password":""
                        }
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .returnResult(String.class)
                .getResponseBody();

        assertThat(response).contains("Validation Error");
        assertThat(response).contains("Invalid email address");
        assertThat(response).contains("Password is required");
    }
}
