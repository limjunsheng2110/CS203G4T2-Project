package com.cs205.tariffg4t2.repository.basic;

import com.cs205.tariffg4t2.model.basic.PreferentialRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreferentialRateRepository extends JpaRepository<PreferentialRate, Long> {

    @Query("SELECT pr FROM PreferentialRate pr " +
           "JOIN pr.tradeAgreement ta " +
           "WHERE pr.originCountry.countryCode = :originCountryCode " +
           "AND pr.destinationCountry.countryCode = :destinationCountryCode " +
           "AND pr.product.hsCode = :hsCode " +
           "AND ta.effectiveDate <= CURRENT_DATE " +
           "AND (ta.expiryDate IS NULL OR ta.expiryDate > CURRENT_DATE)")
    List<PreferentialRate> findActiveRatesBetweenCountriesForProduct(
        @Param("originCountryCode") String originCountryCode,
        @Param("destinationCountryCode") String destinationCountryCode,
        @Param("hsCode") String hsCode
    );

    @Query("SELECT pr FROM PreferentialRate pr " +
           "JOIN pr.tradeAgreement ta " +
           "WHERE pr.originCountry.countryCode = :originCountryCode " +
           "AND pr.destinationCountry.countryCode = :destinationCountryCode " +
           "AND pr.product.hsCode = :hsCode " +
           "AND ta.effectiveDate <= CURRENT_DATE " +
           "AND (ta.expiryDate IS NULL OR ta.expiryDate > CURRENT_DATE) " +
           "ORDER BY pr.preferentialRate ASC")
    Optional<PreferentialRate> findLowestActiveRate(
        @Param("originCountryCode") String originCountryCode,
        @Param("destinationCountryCode") String destinationCountryCode,
        @Param("hsCode") String hsCode
    );
}
