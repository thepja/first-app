package com.example.app.account;

import com.example.app.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inscription, connexion et utilisateur courant. La déconnexion est gérée par Spring Security (SecurityConfig). */
@RestController
@RequestMapping("/api/auth")
class AccountController {

    record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            // Recommandations NIST 800-63B : longueur minimale, pas de règles de composition
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(max = 80) String displayName) {}

    record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    record UserResponse(String email, String displayName) {
        static UserResponse of(CurrentUser user) {
            return new UserResponse(user.getUsername(), user.displayName());
        }
    }

    private final AccountService accounts;
    private final AuthenticationManager authenticationManager;
    private final LoginAttempts loginAttempts;
    private final LoginSessions loginSessions;

    AccountController(
            AccountService accounts,
            AuthenticationManager authenticationManager,
            LoginAttempts loginAttempts,
            LoginSessions loginSessions) {
        this.accounts = accounts;
        this.authenticationManager = authenticationManager;
        this.loginAttempts = loginAttempts;
        this.loginSessions = loginSessions;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(
            @Valid @RequestBody RegisterRequest body, HttpServletRequest request, HttpServletResponse response) {
        CurrentUser user = accounts.register(body.email(), body.password(), body.displayName());
        loginSessions.start(user, request, response);
        return UserResponse.of(user);
    }

    @PostMapping("/login")
    UserResponse login(
            @Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        String email = Emails.normalize(body.email());
        loginAttempts.checkAllowed(email);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, body.password()));
        } catch (AuthenticationException e) {
            loginAttempts.failed(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Adresse e-mail ou mot de passe incorrect.");
        }
        loginAttempts.succeeded(email);
        loginSessions.start(authentication, request, response);
        return UserResponse.of((CurrentUser) authentication.getPrincipal());
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal CurrentUser user) {
        return UserResponse.of(user);
    }
}
