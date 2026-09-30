package com.example.blog.user.web;

import static org.springframework.http.HttpStatus.CREATED;

import com.example.blog.user.domain.UserService;
import com.example.blog.user.domain.model.CreateUserCmd;
import com.example.blog.user.domain.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api")
class UserController {
    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers a new user account.
     *
     * @return the created user's public profile
     */
    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest req) {
        LOG.info("Create user request for email: {}", req.email());
        var cmd = new CreateUserCmd(req.name(), req.email(), req.password(), Role.ROLE_USER);
        userService.createUser(cmd);
        var response = new CreateUserResponse(req.name(), req.email(), Role.ROLE_USER);
        return ResponseEntity.status(CREATED.value()).body(response);
    }

    record CreateUserRequest(
            @NotBlank(message = "{validation.name.required}") String name,

            @NotBlank(message = "{validation.email.required}") @Email(message = "{validation.email.invalid}") String email,

            @NotBlank(message = "{validation.password.required}") String password) {}

    record CreateUserResponse(String name, String email, Role role) {}
}
