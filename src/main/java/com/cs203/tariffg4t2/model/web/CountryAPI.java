package com.cs203.tariffg4t2.model.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CountryAPI {
    @JsonProperty("cca2")  // ISO 2-letter code
    private String code;

    @JsonProperty("name")
    private CountryName name;

    @JsonProperty("region")
    private String region;

    @JsonProperty("currencies")
    private Object currencies;  // Complex structure, we'll handle this

    @Data
    public static class CountryName {
        @JsonProperty("common")
        private String common;
    }
}