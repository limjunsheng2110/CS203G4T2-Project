package com.cs205.tariffg4t2.repository.basic;

import com.cs205.tariffg4t2.model.basic.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface CountryRepository extends JpaRepository<Country, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);

    Optional<Country> findByCodeIgnoreCase(String code);
    Optional<Country> findByNameIgnoreCase(String name);
}

