package com.CS203.tariffg4t2.repository.basic;

import com.CS203.tariffg4t2.model.basic.TradeAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradeAgreementRepository extends JpaRepository<TradeAgreement, Long> {
    Optional<TradeAgreement> findByName(String name);

    @Query("SELECT DISTINCT ta FROM TradeAgreement ta " +
           "JOIN ta.preferentialRates pr " +
           "WHERE pr.exportingCountry.countryCode = :exportingCountryCode " +
           "AND pr.importingCountry.countryCode = :importingCountryCode " +
           "AND pr.product.hsCode = :hsCode " +
           "AND CURRENT_DATE BETWEEN ta.effectiveDate AND ta.expiryDate")
    Optional<TradeAgreement> findTradeAgreementByCountriesAndHsCode(
            @Param("exportingCountryCode") String exportingCountryCode,
            @Param("importingCountryCode") String importingCountryCode,
            @Param("hsCode") String hsCode);
}
