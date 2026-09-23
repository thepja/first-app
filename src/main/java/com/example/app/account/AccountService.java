package com.example.app.account;

import com.example.app.web.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    AccountService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    CurrentUser register(String email, String password, String displayName) {
        String normalized = Emails.normalize(email);
        if (users.existsByEmail(normalized)) {
            throw emailTaken();
        }
        AppUser user;
        try {
            user = users.saveAndFlush(new AppUser(normalized, passwordEncoder.encode(password), displayName.strip()));
        } catch (DataIntegrityViolationException e) {
            throw emailTaken(); // inscription concurrente avec la même adresse
        }
        CurrentUser current = new CurrentUser(user);
        current.eraseCredentials();
        return current;
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cette adresse.");
    }
}
