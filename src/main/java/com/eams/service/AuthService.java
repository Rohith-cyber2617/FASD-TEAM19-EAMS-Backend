package com.eams.service;

import com.eams.dto.SignupRequest;
import com.eams.entity.User;
import com.eams.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initAdmin() {
        if (!userRepository.existsByEmail("admin1@kluniversity.in")) {
            User admin = new User();
            admin.setEmail("admin1@kluniversity.in");
            admin.setPassword(passwordEncoder.encode("FASD@admin1234"));
            admin.setName("System Admin");
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
        }
    }

    public void registerUser(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User.Role role = User.Role.valueOf(request.getRole().toUpperCase());
        if (role == User.Role.ADMIN) {
            throw new IllegalArgumentException("ADMIN cannot be registered through UI");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        userRepository.save(user);
    }
}
