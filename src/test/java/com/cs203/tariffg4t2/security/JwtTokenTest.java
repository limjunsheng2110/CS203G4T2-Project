package com.cs203.tariffg4t2.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenTest {

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private final Long userId = 1L;
    private final String username = "testuser";
    private final String email = "test@example.com";
    private final String role = "USER";

    @BeforeEach
    void setUp() {
        validToken = jwtService.generateToken(userId, username, email, role);
    }

    // ========== TOKEN GENERATION TESTS ==========

    @Test
    void testGenerateToken_Success() {
        String token = jwtService.generateToken(userId, username, email, role);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void testGenerateToken_WithDifferentUserIds() {
        String token1 = jwtService.generateToken(1L, "user1", "user1@test.com", "USER");
        String token2 = jwtService.generateToken(2L, "user2", "user2@test.com", "ADMIN");

        assertNotEquals(token1, token2);
        assertEquals(1L, jwtService.getUserId(token1));
        assertEquals(2L, jwtService.getUserId(token2));
    }

    @Test
    void testGenerateToken_WithAdminRole() {
        String token = jwtService.generateToken(userId, username, email, "ADMIN");
        
        assertNotNull(token);
        assertEquals("ADMIN", jwtService.getRole(token));
    }

    @Test
    void testGenerateToken_WithNullUserId() {
        String token = jwtService.generateToken(null, username, email, role);
        
        assertNotNull(token);
        assertNull(jwtService.getUserId(token));
    }

    @Test
    void testGenerateToken_WithSpecialCharactersInUsername() {
        String specialUsername = "user@#$%123";
        String token = jwtService.generateToken(userId, specialUsername, email, role);
        
        assertEquals(specialUsername, jwtService.getUsername(token));
    }

    // ========== TOKEN VALIDATION TESTS ==========

    @Test
    void testIsTokenValid_ValidToken_ReturnsTrue() {
        assertTrue(jwtService.isTokenValid(validToken));
    }

    @Test
    void testIsTokenValid_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtService.isTokenValid(invalidToken));
    }

    @Test
    void testIsTokenValid_MalformedToken_ReturnsFalse() {
        String malformedToken = "notajwttoken";
        
        assertFalse(jwtService.isTokenValid(malformedToken));
    }

    @Test
    void testIsTokenValid_EmptyToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid(""));
    }

    @Test
    void testIsTokenValid_NullToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    void testIsTokenValid_TamperedToken_ReturnsFalse() {
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + ".tampered." + parts[2];
        
        assertFalse(jwtService.isTokenValid(tamperedToken));
    }

    @Test
    void testIsTokenValid_ModifiedSignature_ReturnsFalse() {
        String[] parts = validToken.split("\\.");
        String modifiedToken = parts[0] + "." + parts[1] + ".modifiedsignature";
        
        assertFalse(jwtService.isTokenValid(modifiedToken));
    }

    // ========== TOKEN PARSING TESTS ==========

    @Test
    void testParseToken_ValidToken_Success() {
        Jws<Claims> jws = jwtService.parseToken(validToken);
        
        assertNotNull(jws);
        assertNotNull(jws.getBody());
        assertEquals(username, jws.getBody().getSubject());
    }

    @Test
    void testParseToken_InvalidToken_ThrowsException() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(JwtException.class, () -> jwtService.parseToken(invalidToken));
    }

    @Test
    void testParseToken_MalformedToken_ThrowsException() {
        assertThrows(MalformedJwtException.class, () -> jwtService.parseToken("malformed"));
    }

    @Test
    void testParseToken_TamperedSignature_ThrowsException() {
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".tampered";
        
        assertThrows(SignatureException.class, () -> jwtService.parseToken(tamperedToken));
    }

    // ========== USERNAME EXTRACTION TESTS ==========

    @Test
    void testGetUsername_ValidToken_ReturnsCorrectUsername() {
        String extractedUsername = jwtService.getUsername(validToken);
        
        assertEquals(username, extractedUsername);
    }

    @Test
    void testGetUsername_DifferentUsernames() {
        String token1 = jwtService.generateToken(1L, "alice", "alice@test.com", "USER");
        String token2 = jwtService.generateToken(2L, "bob", "bob@test.com", "USER");

        assertEquals("alice", jwtService.getUsername(token1));
        assertEquals("bob", jwtService.getUsername(token2));
    }

    @Test
    void testGetUsername_InvalidToken_ThrowsException() {
        assertThrows(JwtException.class, () -> jwtService.getUsername("invalid.token"));
    }

    // ========== USER ID EXTRACTION TESTS ==========

    @Test
    void testGetUserId_ValidToken_ReturnsCorrectId() {
        Long extractedUserId = jwtService.getUserId(validToken);
        
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testGetUserId_WithIntegerValue() {
        // Token might store userId as Integer
        String token = jwtService.generateToken(123L, "user", "user@test.com", "USER");
        
        Long id = jwtService.getUserId(token);
        assertEquals(123L, id);
    }

    @Test
    void testGetUserId_WithLargeId() {
        Long largeId = 999999999L;
        String token = jwtService.generateToken(largeId, username, email, role);
        
        assertEquals(largeId, jwtService.getUserId(token));
    }

    @Test
    void testGetUserId_WithNullId() {
        String token = jwtService.generateToken(null, username, email, role);
        
        assertNull(jwtService.getUserId(token));
    }

    @Test
    void testGetUserId_InvalidToken_ThrowsException() {
        assertThrows(JwtException.class, () -> jwtService.getUserId("invalid.token"));
    }

    // ========== EMAIL EXTRACTION TESTS ==========

    @Test
    void testGetEmail_ValidToken_ReturnsCorrectEmail() {
        String extractedEmail = jwtService.getEmail(validToken);
        
        assertEquals(email, extractedEmail);
    }

    @Test
    void testGetEmail_DifferentEmails() {
        String token1 = jwtService.generateToken(1L, "user1", "user1@test.com", "USER");
        String token2 = jwtService.generateToken(2L, "user2", "user2@test.com", "USER");

        assertEquals("user1@test.com", jwtService.getEmail(token1));
        assertEquals("user2@test.com", jwtService.getEmail(token2));
    }

    @Test
    void testGetEmail_InvalidToken_ThrowsException() {
        assertThrows(JwtException.class, () -> jwtService.getEmail("invalid.token"));
    }

    // ========== ROLE EXTRACTION TESTS ==========

    @Test
    void testGetRole_ValidToken_ReturnsCorrectRole() {
        String extractedRole = jwtService.getRole(validToken);
        
        assertEquals(role, extractedRole);
    }

    @Test
    void testGetRole_AdminRole() {
        String adminToken = jwtService.generateToken(userId, username, email, "ADMIN");
        
        assertEquals("ADMIN", jwtService.getRole(adminToken));
    }

    @Test
    void testGetRole_DifferentRoles() {
        String userToken = jwtService.generateToken(1L, "user", "user@test.com", "USER");
        String adminToken = jwtService.generateToken(2L, "admin", "admin@test.com", "ADMIN");

        assertEquals("USER", jwtService.getRole(userToken));
        assertEquals("ADMIN", jwtService.getRole(adminToken));
    }

    @Test
    void testGetRole_InvalidToken_ThrowsException() {
        assertThrows(JwtException.class, () -> jwtService.getRole("invalid.token"));
    }

    // ========== TOKEN EXPIRATION TESTS ==========

    @Test
    void testIsTokenExpired_ValidToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenExpired(validToken));
    }

    @Test
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() throws Exception {
        // Create a JwtService with very short expiration (1ms)
        JwtService shortExpiryService = new JwtService("mySecretKeyForTestingPurposesOnly1234567890", 1L);
        
        String expiredToken = shortExpiryService.generateToken(userId, username, email, role);
        
        // Wait for token to expire
        Thread.sleep(100);
        
        assertTrue(shortExpiryService.isTokenExpired(expiredToken));
    }

    @Test
    void testIsTokenExpired_InvalidToken_ReturnsFalse() {
        // Invalid tokens return false (not expired, just invalid)
        assertFalse(jwtService.isTokenExpired("invalid.token"));
    }

    @Test
    void testIsTokenExpired_JustGenerated_ReturnsFalse() {
        String freshToken = jwtService.generateToken(userId, username, email, role);
        
        assertFalse(jwtService.isTokenExpired(freshToken));
    }

    // ========== TOKEN CLAIMS TESTS ==========

    @Test
    void testTokenContainsAllClaims() {
        Jws<Claims> jws = jwtService.parseToken(validToken);
        Claims claims = jws.getBody();

        assertNotNull(claims.get("userId"));
        assertEquals(email, claims.get("email"));
        assertEquals(role, claims.get("role"));
        assertEquals(username, claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void testTokenIssuedAtIsBeforeExpiration() {
        Jws<Claims> jws = jwtService.parseToken(validToken);
        Claims claims = jws.getBody();

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        assertTrue(issuedAt.before(expiration));
    }

    @Test
    void testTokenExpirationIsInFuture() {
        Jws<Claims> jws = jwtService.parseToken(validToken);
        Date expiration = jws.getBody().getExpiration();

        assertTrue(expiration.after(new Date()));
    }

    // ========== EDGE CASES ==========

    @Test
    void testGenerateToken_WithVeryLongUsername() {
        String longUsername = "a".repeat(1000);
        String token = jwtService.generateToken(userId, longUsername, email, role);
        
        assertEquals(longUsername, jwtService.getUsername(token));
    }

    @Test
    void testGenerateToken_WithEmptyEmail() {
        String token = jwtService.generateToken(userId, username, "", role);
        
        assertEquals("", jwtService.getEmail(token));
    }

    @Test
    void testGenerateToken_WithUnicodeCharacters() {
        String unicodeUsername = "用户名";
        String token = jwtService.generateToken(userId, unicodeUsername, email, role);
        
        assertEquals(unicodeUsername, jwtService.getUsername(token));
    }

    @Test
    void testTokenConsistency_SameInputs_DifferentTokens() {
        // Tokens generated at different times should be different (due to timestamp)
        String token1 = jwtService.generateToken(userId, username, email, role);
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String token2 = jwtService.generateToken(userId, username, email, role);
        
        assertNotEquals(token1, token2);
    }

    @Test
    void testMultipleTokenValidations_SameToken() {
        // Validating same token multiple times should give consistent results
        assertTrue(jwtService.isTokenValid(validToken));
        assertTrue(jwtService.isTokenValid(validToken));
        assertTrue(jwtService.isTokenValid(validToken));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testFullTokenLifecycle() {
        // Generate token
        String token = jwtService.generateToken(100L, "lifecycle", "lifecycle@test.com", "ADMIN");
        
        // Validate token
        assertTrue(jwtService.isTokenValid(token));
        
        // Extract all claims
        assertEquals(100L, jwtService.getUserId(token));
        assertEquals("lifecycle", jwtService.getUsername(token));
        assertEquals("lifecycle@test.com", jwtService.getEmail(token));
        assertEquals("ADMIN", jwtService.getRole(token));
        
        // Check expiration
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void testTokenWithAllRoleTypes() {
        String userToken = jwtService.generateToken(1L, "user", "user@test.com", "USER");
        String adminToken = jwtService.generateToken(2L, "admin", "admin@test.com", "ADMIN");

        assertTrue(jwtService.isTokenValid(userToken));
        assertTrue(jwtService.isTokenValid(adminToken));
        
        assertEquals("USER", jwtService.getRole(userToken));
        assertEquals("ADMIN", jwtService.getRole(adminToken));
    }

    @Test
    void testTokenSecurityIndependence() {
        // Two users with similar data should have different tokens
        String token1 = jwtService.generateToken(1L, "user", "user@test.com", "USER");
        String token2 = jwtService.generateToken(2L, "user", "user@test.com", "USER");

        assertNotEquals(token1, token2);
        
        // But each should be independently valid
        assertTrue(jwtService.isTokenValid(token1));
        assertTrue(jwtService.isTokenValid(token2));
    }
}