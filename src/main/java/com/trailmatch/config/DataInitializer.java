package com.trailmatch.config;

import com.trailmatch.entity.AdminUser;
import com.trailmatch.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AdminUserRepository repo; private final PasswordEncoder encoder;
    @Value("${app.admin.username}") String username; @Value("${app.admin.password}") String password;
    @Override public void run(String... args) {
        repo.findByUsername(username).orElseGet(() -> repo.save(AdminUser.builder().username(username).passwordHash(encoder.encode(password)).role("ROLE_ADMIN").build()));
    }
}
