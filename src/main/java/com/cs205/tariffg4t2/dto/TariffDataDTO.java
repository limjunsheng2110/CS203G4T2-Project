package com.cs205.tariffg4t2.dto;

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