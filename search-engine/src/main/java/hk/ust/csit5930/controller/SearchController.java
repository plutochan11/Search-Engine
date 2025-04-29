package hk.ust.csit5930.controller;

import hk.ust.csit5930.models.SearchResult;
import hk.ust.csit5930.service.SearchEngineService;
import hk.ust.csit5930.utils.TextProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final SearchEngineService searchEngineService;
    
    @Autowired
    public SearchController(SearchEngineService searchEngineService) {
        this.searchEngineService = searchEngineService;
    }
    
    /**
     * Search API endpoint
     * 
     * @param query the search query
     * @param rankBy optional parameter to specify ranking method (cosine or combined)
     * @return list of search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "combined") String rankBy) {
        
        try {
            logger.info("Received search request: query='{}', rankBy='{}'", query, rankBy);
            
            // Perform search
            Map<String, Object> searchResults = searchEngineService.search(query);
            
            if (searchResults.isEmpty()) {
                logger.info("No results found for query: {}", query);
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            // Choose ranking method based on parameter
            String rankingMethod = rankBy.equalsIgnoreCase("cosine") ? "cosine" : "combined";
            List<Map.Entry<Integer, Object[]>> rankedResults = 
                (List<Map.Entry<Integer, Object[]>>) searchResults.get(rankingMethod);
            
            // Transform into API response format
            List<SearchResult> formattedResults = formatSearchResults(rankedResults, rankingMethod);
            
            return ResponseEntity.ok(formattedResults);
        } catch (Exception e) {
            logger.error("Error processing search request", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process search query");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get details about a specific document
     * 
     * @param docId document ID
     * @return document details
     */
    @GetMapping("/documents/{docId}")
    public ResponseEntity<?> getDocumentDetails(@PathVariable int docId) {
        try {
            Map<String, Object> document = searchEngineService.getDocumentDetails(docId);
            
            if (document.containsKey("error")) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            logger.error("Error retrieving document details", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve document");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get list of all indexed documents
     * 
     * @return list of all documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<Map<String, Object>>> getAllDocuments() {
        try {
            List<Map<String, Object>> documents = searchEngineService.getAllDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Error retrieving all documents", e);
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }
    
    /**
     * Format search results for API response
     */
    private List<SearchResult> formatSearchResults(List<Map.Entry<Integer, Object[]>> results, String rankingMethod) {
        List<SearchResult> formattedResults = new ArrayList<>();
        
        // Process each search result
        for (Map.Entry<Integer, Object[]> entry : results) {
            int docId = entry.getKey();
            Object[] data = entry.getValue();
            
            double cosSimScore = (double) data[0];
            double pageRankScore = 0.0;
            double combinedScore = 0.0;
            List<Integer> positions;
            
            // Extract data based on ranking method
            if (rankingMethod.equals("combined")) {
                pageRankScore = (double) data[1];
                combinedScore = (double) data[2];
                positions = (List<Integer>) data[3];
            } else {
                positions = (List<Integer>) data[1];
            }
            
            // Get document information
            String url = searchEngineService.getDocumentDetails(docId).get("url").toString();
            String title = searchEngineService.getDocumentDetails(docId).get("title").toString();
            
            // Get content snippets
            Vector<String> content = (Vector<String>) searchEngineService.getDocumentDetails(docId).get("content");
            List<String> snippets = positions.stream()
                .map(pos -> TextProcessor.getSurroundingWords(content, pos))
                .distinct()
                .limit(3) // Limit to 3 snippets per document
                .collect(Collectors.toList());
            
            // Use the appropriate score based on ranking method
            double score = rankingMethod.equals("combined") ? combinedScore : cosSimScore;
            
            formattedResults.add(new SearchResult(docId, title, url, score, pageRankScore, snippets));
        }
        
        return formattedResults;
    }
}