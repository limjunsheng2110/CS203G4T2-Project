package com.cs205.tariffg4t2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TariffDataDTO {
    
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
    
    // Default constructor
    public TariffDataDTO() {}
    
    // Full constructor
    public TariffDataDTO(String type, String year, String importedFrom, String exportedFrom, String tariffRate) {
        this.type = type;
        this.year = year;
        this.importedFrom = importedFrom;
        this.exportedFrom = exportedFrom;
        this.tariffRate = tariffRate;
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    
    public String getImportedFrom() { return importedFrom; }
    public void setImportedFrom(String importedFrom) { this.importedFrom = importedFrom; }
    
    public String getExportedFrom() { return exportedFrom; }
    public void setExportedFrom(String exportedFrom) { this.exportedFrom = exportedFrom; }
    
    public String getTariffRate() { return tariffRate; }
    public void setTariffRate(String tariffRate) { this.tariffRate = tariffRate; }
    
    @Override
    public String toString() {
        return "TariffDataDTO{" +
                "type='" + type + '\'' +
                ", year='" + year + '\'' +
                ", importedFrom='" + importedFrom + '\'' +
                ", exportedFrom='" + exportedFrom + '\'' +
                ", tariffRate='" + tariffRate + '\'' +
                '}';
    }
}