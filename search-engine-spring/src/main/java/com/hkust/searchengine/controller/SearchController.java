package com.hkust.searchengine.controller;

import com.hkust.searchengine.model.SearchResult;
import com.hkust.searchengine.service.SearchEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class SearchController {

    private final SearchEngineService searchEngineService;

    @Autowired
    public SearchController(SearchEngineService searchEngineService) {
        this.searchEngineService = searchEngineService;
        // Initialize the search engine when the controller is created
        this.searchEngineService.initialize();
    }
    
    /**
     * Single unified search endpoint that matches the frontend's expectations
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestParam String query, 
                                                   @RequestParam(defaultValue = "combined") String rankBy) {
        List<SearchResult> results;
        
        if ("cosine".equals(rankBy)) {
            results = searchEngineService.searchCosSim(query);
        } else {
            // Default to combined ranking
            results = searchEngineService.searchCombined(query);
        }
        
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/cossim")
    public ResponseEntity<List<SearchResult>> searchCosSim(@RequestParam String query) {
        List<SearchResult> results = searchEngineService.searchCosSim(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/combined")
    public ResponseEntity<List<SearchResult>> searchCombined(@RequestParam String query) {
        List<SearchResult> results = searchEngineService.searchCombined(query);
        return ResponseEntity.ok(results);
    }
    
    /**
     * API initialization status endpoint used by the frontend to check if the search engine is ready
     */
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> getDocuments() {
        boolean isInitialized = searchEngineService.isInitialized();
        Map<String, Object> response = Map.of(
            "initialized", isInitialized,
            "count", searchEngineService.getDocumentCount(),
            "message", isInitialized ? "Search engine initialized successfully" : "Search engine is still initializing"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Search engine API is up and running!");
    }
}