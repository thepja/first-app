package com.example.app.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/** Ouvre la session d'un utilisateur authentifié, comme le ferait le formLogin de Spring Security. */
@Component
class LoginSessions {

    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    LoginSessions(SecurityContextRepository contextRepository, CsrfTokenRepository csrfTokenRepository) {
        this.contextRepository = contextRepository;
        // Nouvel identifiant de session (fixation de session) et nouveau jeton CSRF
        this.sessionStrategy = new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    void start(CurrentUser user, HttpServletRequest request, HttpServletResponse response) {
        start(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()), request, response);
    }

    void start(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        // Émet tout de suite le cookie du nouveau jeton CSRF, pour la prochaine requête du front-end
        if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
    }
}
