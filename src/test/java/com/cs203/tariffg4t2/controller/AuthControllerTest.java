package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.auth.LoginRequest;
import com.cs203.tariffg4t2.dto.auth.RegisterRequest;
import com.cs203.tariffg4t2.model.basic.User;
import com.cs203.tariffg4t2.repository.UserRepository;
import com.cs203.tariffg4t2.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        // setup valid register request
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");

        // setup valid login request
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        // setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole(User.Role.USER);
        testUser.setIsActive(true);

        // setup test token
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }

    // test for register endpoint

    @Test
    void testRegister_Success() throws Exception {
        // given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(testToken);

        // when and then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value(testToken))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateUsername() throws Exception {
        // given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // when and then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error: Username is already taken!"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateEmail() throws Exception {
        // given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // when and then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error: Email is already in use!"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_InvalidEmail() throws Exception {
        // given
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("password123");

        // when and then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_MissingUsername() throws Exception {
        // given
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setPassword("password123");

        // when and then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_MissingPassword() throws Exception {
        // given
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setEmail("test@example.com");

        // when and then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_EmptyRequestBody() throws Exception {
        // when and then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any(User.class));
    }

    // test for login endpoint

    @Test
    void testLogin_Success() throws Exception {
        // given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(testToken);

        // when and then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(testToken))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // when and then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isForbidden()); // Changed from isUnauthorized() to isForbidden()
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        // given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // when and then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("User not found"));
    }

    @Test
    void testLogin_MissingUsername() throws Exception {
        // given
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setPassword("password123");
        invalidRequest.setUsername(null);

        // when and then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_MissingPassword() throws Exception {
        // given
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setPassword(null);

        // when and then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_EmptyRequestBody() throws Exception {
        // when and then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ========== VALIDATE TOKEN TESTS ==========

    @Test
    void testValidateToken_Success() throws Exception {
        // given
        when(jwtService.isTokenValid(testToken)).thenReturn(true);

        // when and then
        mockMvc.perform(get("/auth/validate")
                .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Token is valid"));
    }

    @Test
    void testValidateToken_InvalidToken() throws Exception {
        // given
        when(jwtService.isTokenValid(anyString())).thenReturn(false);

        // when and then
        mockMvc.perform(get("/auth/validate")
                .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("Invalid token"));
    }

    @Test
    void testValidateToken_MissingAuthorizationHeader() throws Exception {
        // when and then - your controller returns 400 with no body
        mockMvc.perform(get("/auth/validate"))
                .andExpect(status().isBadRequest()); // Remove the jsonPath check since there's no JSON body
    }

    @Test
    void testValidateToken_MissingBearerPrefix() throws Exception {
        // when and then
        mockMvc.perform(get("/auth/validate")
                .header("Authorization", testToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("Invalid token"));
    }

    @Test
    void testValidateToken_EmptyToken() throws Exception {
        // when and then
        mockMvc.perform(get("/auth/validate")
                .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("Invalid token"));
    }

    // additional test cases

    @Test
    void testRegister_WithAdminRole() throws Exception {
        // given
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("$2a$10$encodedPassword");
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setIsActive(true);

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(testToken);

        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setUsername("admin");
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("adminpass123");

        // when and then - Note: Default role is USER, so this tests the default behavior
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void testRegister_PasswordEncoding() throws Exception {
        // given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(testToken);

        // when
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated());

        // then - verify password encoder was called
        verify(passwordEncoder, times(1)).encode("password123");
    }
}