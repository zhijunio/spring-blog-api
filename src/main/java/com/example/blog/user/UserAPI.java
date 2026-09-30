package com.example.blog.user;

import com.example.blog.user.domain.model.UserDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface UserAPI {
    Optional<UserDto> findById(Long id);

    Optional<UserDto> findByEmailWithPassword(String email);

    Optional<UserDto> findByEmail(String email);

    List<String> findEmailAddresses(Pageable pageable);
}
