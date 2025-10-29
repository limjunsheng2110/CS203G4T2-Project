package com.cs203.tariffg4t2.dto.scraping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedTariffResponse {
    private String status;
    private String source_url;
    private String chapter;
    private int results_count;
    private List<ScrapedTariffData> data;
}
