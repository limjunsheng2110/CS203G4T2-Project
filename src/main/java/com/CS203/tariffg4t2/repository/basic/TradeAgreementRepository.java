package com.CS203.tariffg4t2.repository.basic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CS203.tariffg4t2.model.basic.TradeAgreement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeAgreementRepository extends JpaRepository<TradeAgreement, Long> {
    Optional<TradeAgreement> findByName(String name);
}
