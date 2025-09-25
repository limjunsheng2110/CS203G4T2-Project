// src/test/java/com/cs205/tariffg4t2/repository/ScrapingRepositoryJobTest.java
package com.cs205.tariffg4t2;

import com.cs205.tariffg4t2.model.web.ScrapingJob;
import com.cs205.tariffg4t2.model.web.TargetUrl;
import com.cs205.tariffg4t2.repository.ScrapingRepositoryJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;




@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScrapingRepositoryJobTest {

    @Autowired
    private ScrapingRepositoryJob scrapingRepositoryJob;

    @Test
    void testFindByStatusOrderByStartTimeDesc() {
        TargetUrl url = new TargetUrl();
        // set url fields if needed

        ScrapingJob job1 = new ScrapingJob();
        job1.setStatus("SUCCESS");
        job1.setStartTime(LocalDateTime.now().minusHours(2));
        job1.setTargetUrl(url);

        ScrapingJob job2 = new ScrapingJob();
        job2.setStatus("SUCCESS");
        job2.setStartTime(LocalDateTime.now());
        job2.setTargetUrl(url);

        scrapingRepositoryJob.save(job1);
        scrapingRepositoryJob.save(job2);

        List<ScrapingJob> jobs = scrapingRepositoryJob.findByStatusOrderByStartTimeDesc("SUCCESS");
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getStartTime()).isAfter(jobs.get(1).getStartTime());
    }
}