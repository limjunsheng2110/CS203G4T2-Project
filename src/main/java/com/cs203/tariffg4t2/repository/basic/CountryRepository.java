package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface CountryRepository extends JpaRepository<Country, String> { // CountryCode is String
    
    // These methods must match your entity field names exactly
    boolean existsByCountryCodeIgnoreCase(String countryCode);
    boolean existsByCountryNameIgnoreCase(String countryName);
    Optional<Country> findByCountryCodeIgnoreCase(String countryCode);
    Optional<Country> findByCountryNameIgnoreCase(String countryName);
    Optional<Country> findByIso3CodeIgnoreCase(String iso3Code);

    // Standard methods
    Optional<Country> findByCountryCode(String countryCode);
    boolean existsByCountryCode(String countryCode);
}