package com.CS203.tariffg4t2.repository.basic;

import com.CS203.tariffg4t2.model.basic.TariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {
    
    // Change this method name to match your entity fields:
    Optional<TariffRate> findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
        String hsCode, String importingCountryCode, String exportingCountryCode);
}