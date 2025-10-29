package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.CountryProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryProfileRepository extends JpaRepository<CountryProfile, String> {
    CountryProfile findByCountryCode(String countryCode);
}
