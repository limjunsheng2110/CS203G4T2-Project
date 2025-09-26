package com.cs205.tariffg4t2.model.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.cs205.tariffg4t2.dto.response.ScraperResponse;

@Service
public class ScrapingClient {

    private final RestTemplate restTemplate;

    @Value("${scraper.api.base-url}")
    private String baseUrl;

    public ScrapingClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public ScraperResponse getScrapedData(String product, String country) {
        String url = String.format("%s/scrape?product=%s&country=%s", baseUrl, product, country);
        return restTemplate.getForObject(url, ScraperResponse.class);
    }
}