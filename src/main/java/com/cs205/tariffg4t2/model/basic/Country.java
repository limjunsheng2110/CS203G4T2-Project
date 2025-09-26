package com.cs205.tariffg4t2.model.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Country {
    @Id
    private String code; //ISO-code (e.g., "US", "CN", "IN")
    private String name;
    private String region;
    private String currency;
    
}


