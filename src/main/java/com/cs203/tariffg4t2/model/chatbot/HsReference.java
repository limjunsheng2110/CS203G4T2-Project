package com.cs203.tariffg4t2.model.chatbot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hs_reference")
public class HsReference {

    @Id
    @Column(name = "hs_code", length = 10, nullable = false)
    private String hsCode;

    @Column(name = "description", length = 1000, nullable = false)
    private String description;

    @Column(name = "section_name", length = 120)
    private String sectionName;

    @Column(name = "chapter_name", length = 120)
    private String chapterName;

    @Column(name = "keywords", length = 300)
    private String keywords;
}

