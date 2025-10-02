package com.CS203.tariffg4t2.repository.basic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CS203.tariffg4t2.model.basic.Country;

import java.util.Optional;


@Repository
public interface CountryRepository extends JpaRepository<Country, String> { // CountryCode is String
    
    // These methods must match your entity field names exactly
    boolean existsByCountryCodeIgnoreCase(String countryCode);
    boolean existsByCountryNameIgnoreCase(String countryName);
    Optional<Country> findByCountryCodeIgnoreCase(String countryCode);
    Optional<Country> findByCountryNameIgnoreCase(String countryName);
    
    // Standard methods
    Optional<Country> findByCountryCode(String countryCode);
    boolean existsByCountryCode(String countryCode);
}