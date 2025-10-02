package com.CS203.tariffg4t2.repository.basic;

import org.springframework.data.geo.Distance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CS203.tariffg4t2.model.basic.ShippingRate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {

    @Query("SELECT sr FROM ShippingRate sr " +
           "WHERE sr.importingCountry.countryCode = :importingCountryCode " +
           "AND sr.exportingCountry.countryCode = :exportingCountryCode")
    Optional<ShippingRate> findByImportingAndExportingCountry(
            @Param("importingCountryCode") String importingCountryCode,
            @Param("exportingCountryCode") String exportingCountryCode);

    @Query("SELECT sr.distance FROM ShippingRate sr " +
            "WHERE sr.importingCountry.countryCode = :importingCountryCode " +
            "AND sr.exportingCountry.countryCode = :exportingCountryCode")
    Optional<BigDecimal> findDistanceByImportingAndExportingCountry(
            @Param("importingCountryCode") String importingCountryCode,
            @Param("exportingCountryCode") String exportingCountryCode);


    @Query("SELECT sr FROM ShippingRate sr WHERE sr.importingCountry.countryCode = :countryCode OR sr.exportingCountry.countryCode = :countryCode")
    List<ShippingRate> findByCountryCode(@Param("countryCode") String countryCode);


}
