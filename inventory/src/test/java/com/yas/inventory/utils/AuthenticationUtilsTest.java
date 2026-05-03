package com.yas.inventory.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_anonymous_throws() {
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication anon = mock(AnonymousAuthenticationToken.class);
        when(ctx.getAuthentication()).thenReturn(anon);
        SecurityContextHolder.setContext(ctx);

        assertThrows(RuntimeException.class, () -> com.yas.inventory.utils.AuthenticationUtils.extractUserId());
    }

    @Test
    void extractUserId_returnsSubject() {
        JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);
        when(auth.getToken()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn("user-123");

        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        String uid = com.yas.inventory.utils.AuthenticationUtils.extractUserId();
        assertEquals("user-123", uid);
    }

    @Test
    void extractJwt_returnsTokenValue() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("tok-val");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        String tok = com.yas.inventory.utils.AuthenticationUtils.extractJwt();
        assertEquals("tok-val", tok);
    }
}
