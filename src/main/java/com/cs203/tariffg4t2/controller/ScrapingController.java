//// Create: src/main/java/com/cs205/tariffg4t2/controller/ScrapingController.java
//
//package com.cs205.tariffg4t2.controller;
//
//import web.model.com.CS203.tariffg4t2.TargetUrl;
//import web.model.com.CS203.tariffg4t2.ScrapingJob;
//import repository.com.CS203.tariffg4t2.TargetUrlRepository;
//import data.service.com.CS203.tariffg4t2.WebScrapingService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/scraping")
//public class ScrapingController {
//
//    @Autowired
//    private TargetUrlRepository targetUrlRepository;
//
//    @Autowired
//    private WebScrapingService webScrapingService;
//
//    @GetMapping("/urls")
//    public List<TargetUrl> getAllUrls() {
//        return targetUrlRepository.findAll();
//    }
//
//    @PostMapping("/urls")
//    public TargetUrl addUrl(@RequestBody TargetUrl targetUrl) {
//        targetUrl.setActive(true);
//        targetUrl.setCreatedAt(LocalDateTime.now());
//        return targetUrlRepository.save(targetUrl);
//    }
//
//    @PostMapping("/scrape/{urlId}")
//    public ResponseEntity<ScrapingJob> scrapeSpecificUrl(@PathVariable Long urlId) {
//        TargetUrl targetUrl = targetUrlRepository.findById(urlId).orElse(null);
//        if (targetUrl == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        ScrapingJob job = webScrapingService.scrapeUrl(targetUrl);
//        return ResponseEntity.ok(job);
//    }
//
//    @PostMapping("/scrape-all")
//    public ResponseEntity<String> scrapeAllUrls() {
//        webScrapingService.scrapeAllDueUrls();
//        return ResponseEntity.ok("Scraping initiated for all due URLs");
//    }
//
//    @GetMapping("/stats")
//    public ResponseEntity<Object> getStats() {
//        return ResponseEntity.ok(webScrapingService.getScrapingStats());
//    }
//}