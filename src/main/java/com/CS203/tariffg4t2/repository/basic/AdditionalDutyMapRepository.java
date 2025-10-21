package com.CS203.tariffg4t2.repository.basic;

import com.CS203.tariffg4t2.model.basic.AdditionalDutyMap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface AdditionalDutyMapRepository extends JpaRepository<AdditionalDutyMap, Long> {
    Optional<AdditionalDutyMap> findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
        String importingCountry, String exportingCountry, String hsCode, LocalDate from, LocalDate to);
}

