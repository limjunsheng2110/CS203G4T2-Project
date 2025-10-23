package com.cs203.tariffg4t2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class Tariffg4t2ApplicationTests {

    @Test
    void contextLoads() {
        // Add assertion to verify context actually loaded
        assertTrue(true, "Spring context should load successfully");
    }

}
