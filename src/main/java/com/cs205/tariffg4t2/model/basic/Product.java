package com.cs205.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {
    
    @Id
    @Column(name = "hs_code", length = 20)
    private String hsCode;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "category", length = 100, nullable = true)
    private String category;
}