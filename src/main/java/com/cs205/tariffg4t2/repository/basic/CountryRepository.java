package com.cs205.tariffg4t2.repository.basic;

import com.cs205.tariffg4t2.model.basic.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
}
