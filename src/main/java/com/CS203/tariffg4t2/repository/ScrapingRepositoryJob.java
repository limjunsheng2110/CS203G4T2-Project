package com.CS203.tariffg4t2.repository;

import com.CS203.tariffg4t2.model.web.ScrapingJob;
import com.CS203.tariffg4t2.model.web.TargetUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface ScrapingRepositoryJob extends JpaRepository<ScrapingJob, Long> {

       // Find scraping jobs by target URL
       List<ScrapingJob> findByTargetUrlOrderByStartTimeDesc(TargetUrl targetUrl);

       // Find scraping jobs by status
       List<ScrapingJob> findByStatusOrderByStartTimeDesc(String status);

       // Find recent scraping jobs (last 24 hours)
       List<ScrapingJob> findByStartTimeAfterOrderByStartTimeDesc(LocalDateTime since);

       // Find failed scraping jobs
       List<ScrapingJob> findByStatusAndStartTimeAfter(String status, LocalDateTime since);

       @Query("SELECT " +
                     "COUNT(CASE WHEN s.status = 'SUCCESS' THEN 1 END) * 100.0 / COUNT(*) " +
                     "FROM ScrapingJob s WHERE s.targetUrl = :targetUrl AND s.startTime > :since")
       Double getSuccessRateForUrl(@Param("targetUrl") TargetUrl targetUrl, @Param("since") LocalDateTime since);

       @Query("SELECT s FROM ScrapingJob s WHERE s.status = 'SUCCESS' AND " +
                     "s.startTime = (SELECT MAX(s2.startTime) FROM ScrapingJob s2 " +
                     "WHERE s2.targetUrl = s.targetUrl AND s2.status = 'SUCCESS')")
       List<ScrapingJob> findLatestSuccessfulScrapingPerUrl();

       @Query("SELECT s FROM ScrapingJob s WHERE s.status = 'IN_PROGRESS' AND s.startTime < :cutoffTime")
       List<ScrapingJob> findStuckScrapingJobs(@Param("cutoffTime") LocalDateTime cutoffTime);
}