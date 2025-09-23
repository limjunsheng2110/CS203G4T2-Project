package com.cs205.tariffg4t2.repository;

import com.cs205.tariffg4t2.model.api.TariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {
    
    // Change this method name to match your entity fields:
    Optional<TariffRate> findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
        String hsCode, String importingCountryCode, String exportingCountryCode);
}