package com.cs205.tariffg4t2.repository.basic;

import com.cs205.tariffg4t2.model.basic.TradeAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeAgreementRepository extends JpaRepository<TradeAgreement, Long> {
    // Additional query methods can be defined here if needed
}
