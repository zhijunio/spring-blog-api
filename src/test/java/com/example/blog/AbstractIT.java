package com.example.blog;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.example.blog.auth.domain.JwtTokenFactory;
import com.example.blog.user.domain.model.Role;
import com.example.blog.user.domain.model.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Sql("/test-data.sql")
public abstract class AbstractIT {

    @Autowired
    protected RestTestClient restTestClient;

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JwtTokenFactory jwtTokenFactory;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    public String adminToken() {
        UserDto userDto = new UserDto(1L, "Administrator", "admin@gmail.com", "", Role.ROLE_ADMIN);
        return this.createToken(userDto);
    }

    public String createToken(UserDto userDto) {
        return jwtTokenFactory.generateToken(userDto).token();
    }
}
