package com.example.app.account;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utilisateur connecté, stocké dans la session (sérialisé en base par Spring Session JDBC).
 *
 * <p>Le hash du mot de passe n'y est présent que le temps de l'authentification, puis effacé.
 */
public final class CurrentUser implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;
    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final long id;
    private final String email;
    private final String displayName;
    private String passwordHash;

    CurrentUser(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.passwordHash = user.getPasswordHash();
    }

    public long id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
