package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.model.chatbot.HsReference;
import com.cs203.tariffg4t2.repository.chatbot.HsReferenceRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HsReferenceDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(HsReferenceDataLoader.class);
    private static final String DATA_PATH = "data/hs_reference_seed.csv";

    private final HsReferenceRepository hsReferenceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (hsReferenceRepository.count() > 0) {
            logger.info("HS reference table already populated ({} records).", hsReferenceRepository.count());
            return;
        }

        try {
            List<HsReference> seedData = loadSeedData();
            hsReferenceRepository.saveAll(seedData);
            logger.info("Seeded {} HS reference records.", seedData.size());
        } catch (IOException e) {
            logger.error("Failed to load HS reference seed data", e);
        }
    }

    private List<HsReference> loadSeedData() throws IOException {
        ClassPathResource resource = new ClassPathResource(DATA_PATH);
        if (!resource.exists()) {
            logger.warn("HS reference seed file {} not found on classpath.", DATA_PATH);
            return List.of();
        }

        List<HsReference> references = new ArrayList<>();

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String[] columns = line.split(",", -1);
                if (columns.length < 3) {
                    continue;
                }

                String hsCode = columns[0].trim();
                String description = columns[1].trim();
                String sectionName = columns.length > 2 ? columns[2].trim() : null;
                String chapterName = columns.length > 3 ? columns[3].trim() : null;
                String keywords = columns.length > 4 ? columns[4].trim() : null;

                if (hsCode.isEmpty() || description.isEmpty()) {
                    continue;
                }

                references.add(HsReference.builder()
                        .hsCode(hsCode)
                        .description(description)
                        .sectionName(sectionName)
                        .chapterName(chapterName)
                        .keywords(keywords)
                        .build());
            }
        }
        return references;
    }
}

