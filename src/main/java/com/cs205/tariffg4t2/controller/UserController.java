//package com.cs205.tariffg4t2.controller;
//
//import com.cs205.tariffg4t2.dto.request.UserRequestDTO;
//import com.cs205.tariffg4t2.dto.basic.UserDTO;
//import com.cs205.tariffg4t2.model.basic.User;
//import com.cs205.tariffg4t2.service.basic.UserService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/users")
//@RequiredArgsConstructor
//public class UserController {
//
//    private final UserService userService;
//
//    /**
//     * GET /api/users - Get all users
//     */
//    @GetMapping
//    public ResponseEntity<List<UserDTO>> getAllUsers() {
//        try {
//            List<UserDTO> users = userService.getAllUsers();
//            return ResponseEntity.ok(users);
//        } catch (Exception e) {
//            log.error("Error fetching all users: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * GET /api/users/{id} - Get user by ID
//     */
//    @GetMapping("/{id}")
//    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
//        try {
//            return userService.getUserById(id)
//                    .map(user -> ResponseEntity.ok(user))
//                    .orElse(ResponseEntity.notFound().build());
//        } catch (Exception e) {
//            log.error("Error fetching user with id {}: {}", id, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * GET /api/users/username/{username} - Get user by username
//     */
//    @GetMapping("/username/{username}")
//    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
//        try {
//            return userService.getUserByUsername(username)
//                    .map(user -> ResponseEntity.ok(user))
//                    .orElse(ResponseEntity.notFound().build());
//        } catch (Exception e) {
//            log.error("Error fetching user with username {}: {}", username, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * GET /api/users/email/{email} - Get user by email
//     */
//    @GetMapping("/email/{email}")
//    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
//        try {
//            return userService.getUserByEmail(email)
//                    .map(user -> ResponseEntity.ok(user))
//                    .orElse(ResponseEntity.notFound().build());
//        } catch (Exception e) {
//            log.error("Error fetching user with email {}: {}", email, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * POST /api/users - Create new user
//     */
//    @PostMapping
//    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequestDTO request) {
//        try {
//            UserDTO createdUser = userService.createUser(request);
//            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
//        } catch (RuntimeException e) {
//            log.error("Error creating user: {}", e.getMessage());
//            return ResponseEntity.badRequest().body("Error creating user: " + e.getMessage());
//        } catch (Exception e) {
//            log.error("Unexpected error creating user: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("An unexpected error occurred");
//        }
//    }
//
//    /**
//     * PUT /api/users/{id} - Update user
//     */
//    @PutMapping("/{id}")
//    public ResponseEntity<?> updateUser(@PathVariable Long id,
//                                       @Valid @RequestBody UserRequestDTO request) {
//        try {
//            UserDTO updatedUser = userService.updateUser(id, request);
//            return ResponseEntity.ok(updatedUser);
//        } catch (RuntimeException e) {
//            log.error("Error updating user with id {}: {}", id, e.getMessage());
//            return ResponseEntity.badRequest().body("Error updating user: " + e.getMessage());
//        } catch (Exception e) {
//            log.error("Unexpected error updating user with id {}: {}", id, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("An unexpected error occurred");
//        }
//    }
//
//    /**
//     * PATCH /api/users/{id}/deactivate - Deactivate user (soft delete)
//     */
//    @PatchMapping("/{id}/deactivate")
//    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
//        try {
//            userService.deactivateUser(id);
//            return ResponseEntity.ok("User deactivated successfully");
//        } catch (RuntimeException e) {
//            log.error("Error deactivating user with id {}: {}", id, e.getMessage());
//            return ResponseEntity.badRequest().body("Error deactivating user: " + e.getMessage());
//        } catch (Exception e) {
//            log.error("Unexpected error deactivating user with id {}: {}", id, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("An unexpected error occurred");
//        }
//    }
//
//    /**
//     * DELETE /api/users/{id} - Permanently delete user
//     */
//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
//        try {
//            userService.deleteUser(id);
//            return ResponseEntity.ok("User deleted successfully");
//        } catch (RuntimeException e) {
//            log.error("Error deleting user with id {}: {}", id, e.getMessage());
//            return ResponseEntity.badRequest().body("Error deleting user: " + e.getMessage());
//        } catch (Exception e) {
//            log.error("Unexpected error deleting user with id {}: {}", id, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("An unexpected error occurred");
//        }
//    }
//
//    /**
//     * GET /api/users/active - Get only active users
//     */
//    @GetMapping("/active")
//    public ResponseEntity<List<UserDTO>> getActiveUsers() {
//        try {
//            List<UserDTO> activeUsers = userService.getActiveUsers();
//            return ResponseEntity.ok(activeUsers);
//        } catch (Exception e) {
//            log.error("Error fetching active users: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * GET /api/users/role/{role} - Get users by role
//     */
//    @GetMapping("/role/{role}")
//    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable User.Role role) {
//        try {
//            List<UserDTO> users = userService.getUsersByRole(role);
//            return ResponseEntity.ok(users);
//        } catch (Exception e) {
//            log.error("Error fetching users with role {}: {}", role, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//}
