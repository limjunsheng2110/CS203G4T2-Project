package com.cs203.tariffg4t2.dto.scraping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedTariffData {
    private String exportingCountry;
    private String importingCountry;
    private String productName;
    private String hsCode;
    private String tariffRate;
}
