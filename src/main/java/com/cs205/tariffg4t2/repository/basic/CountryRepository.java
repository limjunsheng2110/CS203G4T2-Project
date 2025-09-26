package com.cs205.tariffg4t2.repository.basic;

import com.cs205.tariffg4t2.model.basic.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, String> {
    // Additional query methods can be added here if needed
    //Country findByName(String name);
}
