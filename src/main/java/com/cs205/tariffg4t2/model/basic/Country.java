package com.cs205.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "country")
public class Country {
    @Id
    @Column(name = "country_code", length = 10)
    private String countryCode;
    
    @Column(name = "country_name", length = 100)  
    private String countryName;

}