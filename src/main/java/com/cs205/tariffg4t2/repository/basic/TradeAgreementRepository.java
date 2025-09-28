package com.cs205.tariffg4t2.repository.basic;

import com.cs205.tariffg4t2.model.basic.TradeAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeAgreementRepository extends JpaRepository<TradeAgreement, Long> {

    Optional<TradeAgreement> findByName(String name);

    List<TradeAgreement> findByType(String type);

    @Query("SELECT ta FROM TradeAgreement ta WHERE ta.effectiveDate <= :date AND (ta.expiryDate IS NULL OR ta.expiryDate >= :date)")
    List<TradeAgreement> findActiveAgreements(@Param("date") LocalDate date);

    @Query("SELECT ta FROM TradeAgreement ta JOIN ta.memberCountries c WHERE c.countryCode = :countryCode")
    List<TradeAgreement> findByMemberCountry(@Param("countryCode") String countryCode);

    @Query("SELECT ta FROM TradeAgreement ta WHERE SIZE(ta.memberCountries) >= :minMembers")
    List<TradeAgreement> findByMinimumMemberCount(@Param("minMembers") int minMembers);
}
