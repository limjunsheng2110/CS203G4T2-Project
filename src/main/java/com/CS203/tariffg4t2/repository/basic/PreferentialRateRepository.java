package com.CS203.tariffg4t2.repository.basic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CS203.tariffg4t2.model.basic.PreferentialRate;

import java.util.Optional;

@Repository
public interface PreferentialRateRepository extends JpaRepository<PreferentialRate, Long> {

    @Query("SELECT pr FROM PreferentialRate pr " +
           "WHERE pr.originCountry.countryCode = :originCountryCode " +
           "AND pr.destinationCountry.countryCode = :destinationCountryCode " +
           "AND pr.product.hsCode = :hsCode")
    Optional<PreferentialRate> findCustomPreferentialRate(
            @Param("originCountryCode") String originCountryCode,
            @Param("destinationCountryCode") String destinationCountryCode,
            @Param("hsCode") String hsCode);
}
