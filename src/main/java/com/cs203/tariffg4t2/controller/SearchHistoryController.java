package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.model.basic.User;
import com.cs203.tariffg4t2.model.web.SearchHistory;
import com.cs203.tariffg4t2.repository.SearchHistoryRepository;
import com.cs203.tariffg4t2.repository.UserRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;



@RestController
@RequestMapping("/api/search-history")
@SecurityRequirement(name = "Bearer Authentication")
public class SearchHistoryController {
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    public SearchHistoryController(SearchHistoryRepository searchHistoryRepository, UserRepository userRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.userRepository = userRepository;
    }

    // get all search history for the authenticated user
    @GetMapping
    public ResponseEntity<List<SearchHistory>> getMySearchHistory(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        List<SearchHistory> history = searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(user.getId());
        return ResponseEntity.ok(history);
    }

    // get specific search history by ID (check for ownership)
    @GetMapping("/{id}")
    public ResponseEntity<SearchHistory> getSearchHistoryById(@PathVariable Long id,
                                                             Authentication authentication) {
        User user = getUserFromAuthentication(authentication);

        SearchHistory history = searchHistoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Search history not found"));
        
            // check ownership
            if (!history.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        return ResponseEntity.ok(history);
    }

    // delete a search history by ID (check for ownership)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSearchHistory(@PathVariable Long id,
                                               Authentication authentication) {
        User user = getUserFromAuthentication(authentication);

        SearchHistory history = searchHistoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Search history not found"));
        
            // check ownership
            if (!history.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

        searchHistoryRepository.delete(history);
        return ResponseEntity.ok("Search history deleted successfully");
    }

    // helper method to get User from Authentication
    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
    
}
