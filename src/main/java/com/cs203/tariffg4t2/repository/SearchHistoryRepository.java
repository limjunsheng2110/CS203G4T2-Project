package com.cs203.tariffg4t2.repository;

import com.cs203.tariffg4t2.model.web.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserId(Long userId);

    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId);

    void deleteByUserId(Long userId);
}
