package com.yas.commonlibrary.utils;

import com.yas.commonlibrary.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeMessagesAuthenticationTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dateTimeUtils_format_defaultPattern() {
        LocalDateTime dt = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
        String formatted = DateTimeUtils.format(dt);
        assertEquals("02-01-2020_03-04-05", formatted);
    }

    @Test
    void dateTimeUtils_format_customPattern() {
        LocalDateTime dt = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
        String formatted = DateTimeUtils.format(dt, "yyyy");
        assertEquals("2020", formatted);
    }

    @Test
    void messagesUtils_missingResource_returnsKey() {
        String key = "non.existent.key";
        String res = MessagesUtils.getMessage(key, "p1");
        assertEquals(key, res);
    }

    @Test
    void authenticationUtils_extractJwt_and_userId_and_anonymous() {
        // extractJwt: principal is a Jwt
        Jwt jwt = Mockito.mock(Jwt.class);
        Mockito.when(jwt.getTokenValue()).thenReturn("token-123");

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("token-123", AuthenticationUtils.extractJwt());

        // extractUserId: authentication is JwtAuthenticationToken
        Jwt jwt2 = Mockito.mock(Jwt.class);
        Mockito.when(jwt2.getSubject()).thenReturn("user-1");
        JwtAuthenticationToken jwtAuth = Mockito.mock(JwtAuthenticationToken.class);
        Mockito.when(jwtAuth.getToken()).thenReturn(jwt2);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        assertEquals("user-1", AuthenticationUtils.extractUserId());

        // anonymous should throw AccessDeniedException
        AnonymousAuthenticationToken anon = new AnonymousAuthenticationToken("key", "anon",
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANON")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);
    }
}
