package com.cs203.tariffg4t2.dto.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffDataDTO {

    //type of data scraped (category: livestock-beef, etc).
    @JsonProperty("type")
    private String type;

    @JsonProperty("year")
    private String year;
    
    @JsonProperty("importedFrom")
    private String importedFrom;
    
    @JsonProperty("exportedFrom")
    private String exportedFrom;
    
    @JsonProperty("tariffRate")
    private String tariffRate;
}