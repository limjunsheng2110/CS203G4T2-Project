package com.cs205.tariffg4t2.dto;

import com.cs205.tariffg4t2.model.basic.TradeAgreement;
import com.cs205.tariffg4t2.model.basic.Country;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeAgreementDTO {
    private Long id;
    private String name;
    private String type;
    private Set<String> memberCountryNames;
    private Set<String> memberCountryCodes;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private boolean isActive;

    public TradeAgreementDTO(TradeAgreement tradeAgreement) {
        this.id = tradeAgreement.getId();
        this.name = tradeAgreement.getName();
        this.type = tradeAgreement.getType();
        this.effectiveDate = tradeAgreement.getEffectiveDate();
        this.expiryDate = tradeAgreement.getExpiryDate();

        if (tradeAgreement.getMemberCountries() != null) {
            this.memberCountryNames = tradeAgreement.getMemberCountries().stream()
                    .map(Country::getCountryName)
                    .collect(Collectors.toSet());
            this.memberCountryCodes = tradeAgreement.getMemberCountries().stream()
                    .map(Country::getCountryCode)
                    .collect(Collectors.toSet());
        }

        // Check if agreement is currently active
        LocalDate now = LocalDate.now();
        this.isActive = (effectiveDate == null || !effectiveDate.isAfter(now)) &&
                       (expiryDate == null || !expiryDate.isBefore(now));
    }
}
