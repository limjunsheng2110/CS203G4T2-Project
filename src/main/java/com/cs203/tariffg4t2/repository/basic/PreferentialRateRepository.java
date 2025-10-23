package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.PreferentialRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreferentialRateRepository extends JpaRepository<PreferentialRate, Long> {

    @Query("SELECT pr FROM PreferentialRate pr " +
           "WHERE pr.importingCountry.countryCode = :importingCountryCode " +
           "AND pr.exportingCountry.countryCode = :exportingCountryCode " +
           "AND pr.product.hsCode = :hsCode")
    Optional<PreferentialRate> findCustomAdValoremPreferentialRate(
            @Param("importingCountryCode") String importingCountryCode,
            @Param("exportingCountryCode") String exportingCountryCode,
            @Param("hsCode") String hsCode);
}
