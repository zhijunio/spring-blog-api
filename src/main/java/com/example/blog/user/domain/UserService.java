package com.example.blog.user.domain;

import com.example.blog.shared.exception.BadRequestException;
import com.example.blog.user.UserAPI;
import com.example.blog.user.domain.model.CreateUserCmd;
import com.example.blog.user.domain.model.UserDto;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserAPI {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id).map(userMapper::toUserDto);
    }

    @Override
    public List<String> findEmailAddresses(Pageable pageable) {
        return userRepository.findEmailAddresses(pageable);
    }

    @Override
    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(userMapper::toUserDtoWithPassword);
    }

    @Override
    public Optional<UserDto> findByEmailWithPassword(String email) {
        return findByEmail(email);
    }

    @Transactional
    public void createUser(CreateUserCmd cmd) {
        var user = new User();
        user.setName(cmd.name());
        user.setEmail(cmd.email());
        user.setPassword(passwordEncoder.encode(cmd.password()));
        user.setRole(cmd.role());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("User with email %s already exists".formatted(cmd.email()), e);
        }
    }
}
