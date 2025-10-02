package com.CS203.tariffg4t2.service.basic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CS203.tariffg4t2.dto.basic.UserDTO;
import com.CS203.tariffg4t2.dto.request.UserRequestDTO;
import com.CS203.tariffg4t2.model.basic.User;
import com.CS203.tariffg4t2.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        return userRepository.findById(id)
                .map(UserDTO::new);
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByUsername(String username) {
        log.info("Fetching user with username: {}", username);
        return userRepository.findByUsername(username)
                .map(UserDTO::new);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        return userRepository.findByEmail(email)
                .map(UserDTO::new);
    }

    /**
     * Create new user
     */
    public UserDTO createUser(UserRequestDTO request) {
        log.info("Creating user with username: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // TODO: Hash password in production
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());

        return new UserDTO(savedUser);
    }

    /**
     * Update user
     */
    public UserDTO updateUser(Long id, UserRequestDTO request) {
        log.info("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Check if username is being changed and already exists
        if (!user.getUsername().equals(request.getUsername()) &&
            userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        // Check if email is being changed and already exists
        if (!user.getEmail().equals(request.getEmail()) &&
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(request.getPassword()); // TODO: Hash password in production
        }
        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with id: {}", updatedUser.getId());

        return new UserDTO(updatedUser);
    }

    /**
     * Delete user (soft delete by setting isActive to false)
     */
    public void deactivateUser(Long id) {
        log.info("Deactivating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated successfully with id: {}", id);
    }

    /**
     * Permanently delete user
     */
    public void deleteUser(Long id) {
        log.info("Permanently deleting user with id: {}", id);

        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted permanently with id: {}", id);
    }

    /**
     * Get active users only
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getActiveUsers() {
        log.info("Fetching active users");
        return userRepository.findByIsActive(true)
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get users by role
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(User.Role role) {
        log.info("Fetching users with role: {}", role);
        return userRepository.findByRole(role)
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }
}
