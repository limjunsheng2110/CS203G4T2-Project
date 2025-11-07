package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.basic.UserDTO;
import com.cs203.tariffg4t2.dto.request.UserRequestDTO;
import com.cs203.tariffg4t2.model.basic.User;
import com.cs203.tariffg4t2.service.basic.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDTO userDTO1;
    private UserDTO userDTO2;
    private UserRequestDTO validUserRequest;
    private List<UserDTO> userList;

    @BeforeEach
    void setUp() {
        // Setup test user DTOs
        userDTO1 = new UserDTO();
        userDTO1.setId(1L);
        userDTO1.setUsername("testuser");
        userDTO1.setEmail("test@example.com");
        userDTO1.setRole(User.Role.USER); 
        userDTO1.setIsActive(true);
        userDTO1.setCreatedAt(LocalDateTime.now());
        userDTO1.setUpdatedAt(LocalDateTime.now());

        userDTO2 = new UserDTO();
        userDTO2.setId(2L);
        userDTO2.setUsername("adminuser");
        userDTO2.setEmail("admin@example.com");
        userDTO2.setRole(User.Role.ADMIN);
        userDTO2.setIsActive(true);
        userDTO2.setCreatedAt(LocalDateTime.now());
        userDTO2.setUpdatedAt(LocalDateTime.now());

        userList = Arrays.asList(userDTO1, userDTO2);

        // Setup valid user request
        validUserRequest = new UserRequestDTO();
        validUserRequest.setUsername("newuser");
        validUserRequest.setEmail("newuser@example.com");
        validUserRequest.setPassword("password123");
    }

    // ========== GET ALL USERS TESTS ==========

    @Test
    void testGetAllUsers_Success() throws Exception {
        // given
        when(userService.getAllUsers()).thenReturn(userList);

        // when and then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[1].username").value("adminuser"));
    }

    @Test
    void testGetAllUsers_EmptyList() throws Exception {
        // given
        when(userService.getAllUsers()).thenReturn(Arrays.asList());

        // when and then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetAllUsers_ServiceException() throws Exception {
        // given
        when(userService.getAllUsers()).thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isInternalServerError());
    }

    // ========== GET USER BY ID TESTS ==========

    @Test
    void testGetUserById_Success() throws Exception {
        // given
        when(userService.getUserById(1L)).thenReturn(Optional.of(userDTO1));

        // when and then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        // given
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        // when and then
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserById_ServiceException() throws Exception {
        // given
        when(userService.getUserById(anyLong())).thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isInternalServerError());
    }

    // ========== GET USER BY USERNAME TESTS ==========

    @Test
    void testGetUserByUsername_Success() throws Exception {
        // given
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(userDTO1));

        // when and then
        mockMvc.perform(get("/api/users/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testGetUserByUsername_NotFound() throws Exception {
        // given
        when(userService.getUserByUsername("nonexistent")).thenReturn(Optional.empty());

        // when and then
        mockMvc.perform(get("/api/users/username/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserByUsername_ServiceException() throws Exception {
        // given
        when(userService.getUserByUsername(anyString())).thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(get("/api/users/username/testuser"))
                .andExpect(status().isInternalServerError());
    }

    // GET user endpoint by email tests

    @Test
    void testGetUserByEmail_Success() throws Exception {
        // given
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(userDTO1));

        // when and then
        mockMvc.perform(get("/api/users/email/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void testGetUserByEmail_NotFound() throws Exception {
        // given
        when(userService.getUserByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // when and then
        mockMvc.perform(get("/api/users/email/nonexistent@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserByEmail_ServiceException() throws Exception {
        // given
        when(userService.getUserByEmail(anyString())).thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(get("/api/users/email/test@example.com"))
                .andExpect(status().isInternalServerError());
    }

    // ========== CREATE USER TESTS ==========

    @Test
    void testCreateUser_Success() throws Exception {
        // given
        UserDTO createdUser = new UserDTO();
        createdUser.setId(3L);
        createdUser.setUsername("newuser");
        createdUser.setEmail("newuser@example.com");
        createdUser.setRole(User.Role.USER);
        createdUser.setIsActive(true);
        createdUser.setCreatedAt(LocalDateTime.now());
        createdUser.setUpdatedAt(LocalDateTime.now());

        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(createdUser);

        // when and then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService, times(1)).createUser(any(UserRequestDTO.class));
    }

    @Test
    void testCreateUser_DuplicateUsername() throws Exception {
        // given
        when(userService.createUser(any(UserRequestDTO.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        // when and then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error creating user: Username already exists"));
    }

    @Test
    void testCreateUser_InvalidEmail() throws Exception {
        // given
        UserRequestDTO invalidRequest = new UserRequestDTO();
        invalidRequest.setUsername("testuser");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("password123");

        when(userService.createUser(any(UserRequestDTO.class)))
                .thenThrow(new RuntimeException("Invalid email format"));

        // when and then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_MissingRequiredFields() throws Exception {
        // given
        UserRequestDTO invalidRequest = new UserRequestDTO();
        // missing username, email, password

        // when and then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // test update user endpoints

    @Test
    void testUpdateUser_Success() throws Exception {
        // given
        UserDTO updatedUser = new UserDTO();
        updatedUser.setId(1L);
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setRole(User.Role.USER);
        updatedUser.setIsActive(true);
        updatedUser.setCreatedAt(LocalDateTime.now());
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userService.updateUser(eq(1L), any(UserRequestDTO.class))).thenReturn(updatedUser);

        // when and then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        verify(userService, times(1)).updateUser(eq(1L), any(UserRequestDTO.class));
    }

    @Test
    void testUpdateUser_NotFound() throws Exception {
        // given
        when(userService.updateUser(eq(999L), any(UserRequestDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        // when and then
        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error updating user: User not found"));
    }

    @Test
    void testUpdateUser_DuplicateEmail() throws Exception {
        // given
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class)))
                .thenThrow(new RuntimeException("Email already in use"));

        // when and then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error updating user: Email already in use"));
    }

    // deactivate user tests

    @Test
    void testDeactivateUser_Success() throws Exception {
        // given
        doNothing().when(userService).deactivateUser(1L);

        // when and then
        mockMvc.perform(patch("/api/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("User deactivated successfully"));

        verify(userService, times(1)).deactivateUser(1L);
    }

    @Test
    void testDeactivateUser_NotFound() throws Exception {
        // given
        doThrow(new RuntimeException("User not found")).when(userService).deactivateUser(999L);

        // when and then
        mockMvc.perform(patch("/api/users/999/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error deactivating user: User not found"));
    }

    @Test
    void testDeactivateUser_AlreadyInactive() throws Exception {
        // given
        doThrow(new RuntimeException("User is already inactive")).when(userService).deactivateUser(1L);

        // when and then
        mockMvc.perform(patch("/api/users/1/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error deactivating user: User is already inactive"));
    }

    // ========== DELETE USER TESTS ==========

    @Test
    void testDeleteUser_Success() throws Exception {
        // given
        doNothing().when(userService).deleteUser(1L);

        // when and then
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("User deleted successfully"));

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void testDeleteUser_NotFound() throws Exception {
        // given
        doThrow(new RuntimeException("User not found")).when(userService).deleteUser(999L);

        // when and then
        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error deleting user: User not found"));
    }

    @Test
    void testDeleteUser_ServiceException() throws Exception {
        // given
        doThrow(new RuntimeException("Cannot delete user with active sessions"))
                .when(userService).deleteUser(1L);

        // when and then
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Error deleting user: Cannot delete user with active sessions"));
    }

    // ========== GET ACTIVE USERS TESTS ==========

    @Test
    void testGetActiveUsers_Success() throws Exception {
        // given
        when(userService.getActiveUsers()).thenReturn(userList);

        // when and then
        mockMvc.perform(get("/api/users/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].isActive").value(true));
    }

    @Test
    void testGetActiveUsers_EmptyList() throws Exception {
        // given
        when(userService.getActiveUsers()).thenReturn(Arrays.asList());

        // when and then
        mockMvc.perform(get("/api/users/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetActiveUsers_ServiceException() throws Exception {
        // given
        when(userService.getActiveUsers()).thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(get("/api/users/active"))
                .andExpect(status().isInternalServerError());
    }

    // get user by role tests

    @Test
    void testGetUsersByRole_Success() throws Exception {
        // given
        List<UserDTO> adminUsers = Arrays.asList(userDTO2);
        when(userService.getUsersByRole(User.Role.ADMIN)).thenReturn(adminUsers);

        // when and then
        mockMvc.perform(get("/api/users/role/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].username").value("adminuser"));
    }

    @Test
    void testGetUsersByRole_UserRole() throws Exception {
        // given
        List<UserDTO> regularUsers = Arrays.asList(userDTO1);
        when(userService.getUsersByRole(User.Role.USER)).thenReturn(regularUsers);

        // when and then
        mockMvc.perform(get("/api/users/role/USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    void testGetUsersByRole_NoUsersWithRole() throws Exception {
        // given
        when(userService.getUsersByRole(User.Role.ADMIN)).thenReturn(Arrays.asList());

        // when and then
        mockMvc.perform(get("/api/users/role/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetUsersByRole_ServiceException() throws Exception {
        // given
        when(userService.getUsersByRole(any(User.Role.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(get("/api/users/role/USER"))
                .andExpect(status().isInternalServerError());
    }
}