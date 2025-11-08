package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.TariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {
    
    // Change to return List to handle multiple years
    List<TariffRate> findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
        String hsCode, String importingCountryCode, String exportingCountryCode);

    // New method with year
    Optional<TariffRate> findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
        String hsCode, String importingCountryCode, String exportingCountryCode, Integer year);

    // Find all tariff rates for a specific HS code and country pair (to find closest year)
    List<TariffRate> findByHsCodeAndImportingCountryCodeAndExportingCountryCodeOrderByYearDesc(
        String hsCode, String importingCountryCode, String exportingCountryCode);

    // Find the closest year available for a specific HS code and country pair
    @Query("SELECT t FROM TariffRate t WHERE t.hsCode = :hsCode " +
           "AND t.importingCountryCode = :importingCountry " +
           "AND t.exportingCountryCode = :exportingCountry " +
           "AND t.year IS NOT NULL " +
           "ORDER BY ABS(t.year - :requestedYear) ASC")
    List<TariffRate> findClosestYearTariffRate(
        @Param("hsCode") String hsCode,
        @Param("importingCountry") String importingCountryCode,
        @Param("exportingCountry") String exportingCountryCode,
        @Param("requestedYear") Integer requestedYear);
}