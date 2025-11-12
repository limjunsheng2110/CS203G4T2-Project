package com.cs203.tariffg4t2.repository.chatbot;

import com.cs203.tariffg4t2.model.chatbot.HsReference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HsReferenceRepository extends JpaRepository<HsReference, String> {

    List<HsReference> findTop5ByDescriptionContainingIgnoreCase(String description);

    @Query("""
            SELECT h FROM HsReference h
            WHERE LOWER(h.description) LIKE LOWER(CONCAT('%', :token, '%'))
               OR LOWER(h.keywords) LIKE LOWER(CONCAT('%', :token, '%'))
            """)
    List<HsReference> searchByToken(@Param("token") String token);
}

