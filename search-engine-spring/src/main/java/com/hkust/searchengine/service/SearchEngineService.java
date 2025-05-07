package com.hkust.searchengine.service;

import com.hkust.searchengine.model.SearchResult;
import com.hkust.searchengine.model.TermData;
import com.hkust.searchengine.model.TermInfo;
import hk.ust.csit5930.model.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchEngineService {
    
    private final StopStemService stopStemService;
    private final CrawlerService crawlerService;
    
    private Map<String, TermInfo> termToTermId;
    private Map<Integer, Double> pageRankScores;
    private int documentSize;
    private Map<Integer, List<Integer>> indexedDocs;
    
    @Value("${search.engine.root-url:https://www.cse.ust.hk/}")
    private String rootUrl;
    
    @Value("${search.engine.max-pages:100}")
    private int maxPages;
    
    @Value("${search.engine.max-depth:2}")
    private int maxDepth;
    
    private boolean isInitialized = false;
    
    @Autowired
    public SearchEngineService(StopStemService stopStemService, CrawlerService crawlerService) {
        this.stopStemService = stopStemService;
        this.crawlerService = crawlerService;
        this.termToTermId = new HashMap<>();
        this.pageRankScores = new HashMap<>();
        this.indexedDocs = new HashMap<>();
    }
    
    /**
     * Initialize the search engine with real data from the crawler
     */
    public void initialize() {
        if (isInitialized) {
            log.info("Search engine already initialized");
            return;
        }
        
        log.info("Initializing search engine with crawler data");
        
        try {
            // Initialize the crawler with configuration from application.properties
            crawlerService.initialize(rootUrl, maxPages, maxDepth);
            
            // Wait until crawler is initialized
            if (!crawlerService.isInitialized()) {
                log.error("Crawler initialization failed");
                return;
            }
            
            // Get all crawled pages
            List<Page> pages = crawlerService.getAllPages();
            this.documentSize = pages.size();
            
            log.info("Building search index from {} pages", documentSize);
            
            // Build term index
            buildTermIndex(pages);
            
            // Build page relationships
            buildPageRelationships();
            
            // Calculate PageRank scores
            calculatePageRank();
            
            log.info("Search engine initialization complete");
            isInitialized = true;
        } catch (Exception e) {
            log.error("Error initializing search engine", e);
            throw new RuntimeException("Failed to initialize search engine", e);
        }
    }
    
    /**
     * Build term index from crawled pages
     */
    private void buildTermIndex(List<Page> pages) {
        log.info("Building term index");
        
        int termId = 1;
        
        for (Page page : pages) {
            int pageId = page.getId();
            String title = page.getTitle();
            String content = page.getContent();
            
            // Process title terms
            if (title != null && !title.isEmpty()) {
                Vector<String> titleWords = new Vector<>(Arrays.asList(title.split("\\s+")));
                Map<String, Integer> processedTitleWords = processWords(titleWords);
                
                for (Map.Entry<String, Integer> entry : processedTitleWords.entrySet()) {
                    String term = entry.getKey();
                    int frequency = entry.getValue();
                    
                    // Add to term index if not already present
                    if (!termToTermId.containsKey(term)) {
                        termToTermId.put(term, new TermInfo(term, termId++, frequency));
                    } else {
                        TermInfo existingTermInfo = termToTermId.get(term);
                        existingTermInfo.setFrequency(existingTermInfo.getFrequency() + frequency);
                    }
                }
            }
            
            // Process content terms
            if (content != null && !content.isEmpty()) {
                Vector<String> contentWords = new Vector<>(Arrays.asList(content.split("\\s+")));
                Map<String, Integer> processedContentWords = processWords(contentWords);
                
                for (Map.Entry<String, Integer> entry : processedContentWords.entrySet()) {
                    String term = entry.getKey();
                    int frequency = entry.getValue();
                    
                    // Add to term index if not already present
                    if (!termToTermId.containsKey(term)) {
                        termToTermId.put(term, new TermInfo(term, termId++, frequency));
                    } else {
                        TermInfo existingTermInfo = termToTermId.get(term);
                        existingTermInfo.setFrequency(existingTermInfo.getFrequency() + frequency);
                    }
                }
            }
        }
        
        log.info("Term index built with {} terms", termToTermId.size());
    }
    
    /**
     * Build page relationships for PageRank calculation
     */
    private void buildPageRelationships() {
        log.info("Building page relationships");
        
        for (Page page : crawlerService.getAllPages()) {
            int pageId = page.getId();
            List<Integer> childIds = crawlerService.getChildPageIds(pageId);
            indexedDocs.put(pageId, childIds);
        }
        
        log.info("Page relationships built for {} pages", indexedDocs.size());
    }
    
    /**
     * Calculate PageRank scores for all pages
     */
    private void calculatePageRank() {
        log.info("Calculating PageRank scores");
        
        int[][] linkMatrix = crawlerService.getLinkMatrix();
        
        // Use a damping factor of 0.85 and iterate 100 times for convergence
        double d = 0.85;
        int iterations = 100;
        int n = crawlerService.getPageCount();
        
        // Initialize PageRank scores to 1/n
        double[] pr = new double[n];
        Arrays.fill(pr, 1.0 / n);
        
        // Iterate to convergence
        for (int iter = 0; iter < iterations; iter++) {
            double[] newPr = new double[n];
            
            for (int i = 0; i < n; i++) {
                double sum = 0.0;
                for (int j = 0; j < n; j++) {
                    if (linkMatrix[j][i] == 1) {
                        // Count outlinks from page j
                        int outlinks = 0;
                        for (int k = 0; k < n; k++) {
                            if (linkMatrix[j][k] == 1) {
                                outlinks++;
                            }
                        }
                        
                        // Avoid division by zero
                        if (outlinks > 0) {
                            sum += pr[j] / outlinks;
                        }
                    }
                }
                
                newPr[i] = (1 - d) / n + d * sum;
            }
            
            // Update PageRank vector
            pr = newPr;
        }
        
        // Store PageRank scores in the map
        for (int i = 0; i < n; i++) {
            pageRankScores.put(i + 1, pr[i]);
        }
        
        log.info("PageRank scores calculated for {} pages", pageRankScores.size());
    }
    
    /**
     * Process words (remove stopwords, stem)
     */
    private Map<String, Integer> processWords(Vector<String> words) {
        Map<String, Integer> result = new HashMap<>();
        
        for (String word : words) {
            // Convert to lowercase and remove non-alphanumeric characters
            word = word.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            
            if (word.isEmpty() || stopStemService.isStopWord(word)) {
                continue;
            }
            
            String stemmed = stopStemService.stem(word);
            if (!stemmed.isEmpty()) {
                result.put(stemmed, result.getOrDefault(stemmed, 0) + 1);
            }
        }
        
        return result;
    }
    
    /**
     * Check if the search engine is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Get the number of documents in the index
     */
    public int getDocumentCount() {
        return documentSize;
    }
    
    /**
     * Search using cosine similarity
     */
    public List<SearchResult> searchCosSim(String userQuery) {
        if (!isInitialized) {
            throw new IllegalStateException("Search engine not initialized");
        }
        
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        log.info("Performing cosine similarity search for query: {}", userQuery);
        
        // Extract quoted phrases before preprocessing
        List<String> quotedPhrases = extractQuotedPhrases(userQuery);
        
        // Process query
        List<String> processedQuery = preprocessQuery(userQuery);
        if (processedQuery.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Calculate cosine similarity scores
        Map<Integer, Double> scores = calculateCosineSimilarity(processedQuery);
        
        // Convert results to SearchResult objects
        return convertScoresToSearchResults(scores, quotedPhrases, false);
    }
    
    /**
     * Search using combined ranking (cosine similarity * PageRank)
     */
    public List<SearchResult> searchCombined(String userQuery) {
        if (!isInitialized) {
            throw new IllegalStateException("Search engine not initialized");
        }
        
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        log.info("Performing combined search for query: {}", userQuery);
        
        // Extract quoted phrases before preprocessing
        List<String> quotedPhrases = extractQuotedPhrases(userQuery);
        
        // Process query
        List<String> processedQuery = preprocessQuery(userQuery);
        if (processedQuery.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Calculate cosine similarity scores
        Map<Integer, Double> cosineScores = calculateCosineSimilarity(processedQuery);
        
        // Combine with PageRank scores
        Map<Integer, Double> combinedScores = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : cosineScores.entrySet()) {
            int pageId = entry.getKey();
            double cosineScore = entry.getValue();
            double pageRankScore = pageRankScores.getOrDefault(pageId, 0.0);
            double combinedScore = cosineScore * pageRankScore;
            combinedScores.put(pageId, combinedScore);
        }
        
        // Convert results to SearchResult objects
        return convertScoresToSearchResults(combinedScores, quotedPhrases, true);
    }
    
    /**
     * Calculate cosine similarity between query and documents
     */
    private Map<Integer, Double> calculateCosineSimilarity(List<String> processedQuery) {
        // This is a simplified cosine similarity calculation
        // In a real implementation, you would use TF-IDF and vector space model
        
        Map<Integer, Double> scores = new HashMap<>();
        
        // Count term frequencies in the query
        Map<String, Integer> queryTermFreq = new HashMap<>();
        for (String term : processedQuery) {
            queryTermFreq.put(term, queryTermFreq.getOrDefault(term, 0) + 1);
        }
        
        // For each page, calculate cosine similarity
        for (Page page : crawlerService.getAllPages()) {
            int pageId = page.getId();
            
            // Get page content and title
            String content = page.getContent() != null ? page.getContent() : "";
            String title = page.getTitle() != null ? page.getTitle() : "";
            
            // Process page content and title
            Vector<String> contentVector = new Vector<>(Arrays.asList(content.split("\\s+")));
            Map<String, Integer> contentTerms = processWords(contentVector);
            
            Vector<String> titleVector = new Vector<>(Arrays.asList(title.split("\\s+")));
            Map<String, Integer> titleTerms = processWords(titleVector);
            
            // Calculate cosine similarity score
            double score = 0.0;
            
            // Weight title terms more heavily (60%)
            for (Map.Entry<String, Integer> entry : queryTermFreq.entrySet()) {
                String term = entry.getKey();
                int queryFreq = entry.getValue();
                
                if (titleTerms.containsKey(term)) {
                    score += 0.6 * queryFreq * titleTerms.get(term);
                }
                
                if (contentTerms.containsKey(term)) {
                    score += 0.4 * queryFreq * contentTerms.get(term);
                }
            }
            
            // Normalize by document length (simplified)
            double docLength = Math.sqrt(titleTerms.values().stream().mapToInt(i -> i * i).sum() +
                                         contentTerms.values().stream().mapToInt(i -> i * i).sum());
            
            if (docLength > 0) {
                score = score / docLength;
                
                // Only include documents with non-zero scores
                if (score > 0) {
                    scores.put(pageId, score);
                }
            }
        }
        
        return scores;
    }
    
    /**
     * Convert scores to SearchResult objects
     */
    private List<SearchResult> convertScoresToSearchResults(
            Map<Integer, Double> scores, List<String> quotedPhrases, boolean isCombinedSearch) {
        
        List<SearchResult> results = new ArrayList<>();
        
        // Sort by score in descending order
        List<Map.Entry<Integer, Double>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort(Map.Entry.<Integer, Double>comparingByValue().reversed());
        
        // Limit to top 50 results
        sortedScores = sortedScores.stream().limit(50).collect(Collectors.toList());
        
        for (Map.Entry<Integer, Double> entry : sortedScores) {
            int pageId = entry.getKey();
            double score = entry.getValue();
            
            // Get page details
            Page page = crawlerService.getPageById(pageId);
            if (page == null) {
                continue;
            }
            
            String title = page.getTitle() != null ? page.getTitle() : "Untitled";
            String url = page.getUrl();
            
            // Last modified time
            Date lastModified = page.getLastModified() != null ? page.getLastModified() : new Date();
            
            // Page size
            long pageSize = page.getContent() != null ? page.getContent().length() : 0;
            
            // Extract keywords with frequencies
            List<Map.Entry<String, Integer>> topTerms = extractTopTerms(page, 10);
            
            // Get title term positions (simplified)
            Map<String, List<Integer>> titleTermPositions = new HashMap<>();
            for (Map.Entry<String, Integer> term : topTerms) {
                String processedTerm = term.getKey();
                titleTermPositions.put(processedTerm, findPositions(processedTerm, page.getTitle()));
            }
            
            // Get body term positions (simplified)
            Map<String, List<Integer>> bodyTermPositions = new HashMap<>();
            for (Map.Entry<String, Integer> term : topTerms) {
                String processedTerm = term.getKey();
                bodyTermPositions.put(processedTerm, findPositions(processedTerm, page.getContent()));
            }
            
            // Get parent/child URLs
            List<String> parentUrls = crawlerService.getParentPages(pageId).stream()
                    .map(Page::getUrl)
                    .collect(Collectors.toList());
            
            List<String> childUrls = crawlerService.getChildPages(pageId).stream()
                    .map(Page::getUrl)
                    .collect(Collectors.toList());
            
            // Generate snippets
            List<String> snippets = generateSnippets(page, topTerms);
            
            // Create SearchResult object
            SearchResult result = new SearchResult(pageId, title, url, score,
                    lastModified, pageSize, topTerms, titleTermPositions, bodyTermPositions,
                    parentUrls, childUrls, snippets);
            
            // Set PageRank score
            double pageRankScore = pageRankScores.getOrDefault(pageId, 0.0);
            result.setPageRankScore(pageRankScore);
            
            // Set combined score for combined search
            if (isCombinedSearch) {
                result.setCombinedScore(score);
            } else {
                result.setCombinedScore(score); // For cosine similarity search, use the same score
            }
            
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * Extract top terms from a page with their frequencies
     */
    private List<Map.Entry<String, Integer>> extractTopTerms(Page page, int limit) {
        Map<String, Integer> termFrequencies = new HashMap<>();
        
        // Process title
        if (page.getTitle() != null && !page.getTitle().isEmpty()) {
            Vector<String> titleVector = new Vector<>(Arrays.asList(page.getTitle().split("\\s+")));
            Map<String, Integer> titleTerms = processWords(titleVector);
            
            // Add title terms with higher weight
            for (Map.Entry<String, Integer> entry : titleTerms.entrySet()) {
                termFrequencies.put(entry.getKey(), entry.getValue() * 2); // Weight title terms more
            }
        }
        
        // Process content
        if (page.getContent() != null && !page.getContent().isEmpty()) {
            Vector<String> contentVector = new Vector<>(Arrays.asList(page.getContent().split("\\s+")));
            Map<String, Integer> contentTerms = processWords(contentVector);
            
            // Add content terms
            for (Map.Entry<String, Integer> entry : contentTerms.entrySet()) {
                termFrequencies.put(entry.getKey(),
                        termFrequencies.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }
        
        // Sort by frequency and limit to top terms
        List<Map.Entry<String, Integer>> topTerms = new ArrayList<>(termFrequencies.entrySet());
        topTerms.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        
        return topTerms.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Find positions of a term in text
     */
    private List<Integer> findPositions(String term, String text) {
        if (text == null || text.isEmpty() || term == null || term.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Integer> positions = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            if (words[i].contains(term.toLowerCase())) {
                positions.add(i);
            }
        }
        
        return positions;
    }
    
    /**
     * Generate snippets for a page
     */
    private List<String> generateSnippets(Page page, List<Map.Entry<String, Integer>> topTerms) {
        if (page.getContent() == null || page.getContent().isEmpty() || topTerms.isEmpty()) {
            return Collections.singletonList("No content available");
        }
        
        List<String> snippets = new ArrayList<>();
        String content = page.getContent();
        String[] sentences = content.split("[.!?]+");
        
        // Get top 3 terms
        List<String> topTermWords = topTerms.stream()
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // Find sentences containing the top terms
        for (String term : topTermWords) {
            for (String sentence : sentences) {
                if (sentence.toLowerCase().contains(term.toLowerCase())) {
                    // Clean up the sentence
                    String snippet = sentence.trim().replaceAll("\\s+", " ");
                    
                    // Add term information
                    snippet = "[Keyword: " + term + "] " + snippet;
                    
                    snippets.add(snippet);
                    break; // Only one snippet per term
                }
            }
        }
        
        // If no snippets found, include the first sentence as a fallback
        if (snippets.isEmpty() && sentences.length > 0) {
            snippets.add(sentences[0].trim().replaceAll("\\s+", " "));
        }
        
        return snippets;
    }
    
    /**
     * Preprocess the user query (tokenization, stopword removal, stemming)
     */
    private List<String> preprocessQuery(String userQuery) {
        String[] queryTokens = userQuery.split("\\s+");
        List<String> result = new ArrayList<>();
        
        for (String token : queryTokens) {
            // Skip quoted terms for exact matching
            if (token.startsWith("\"") || token.endsWith("\"")) {
                continue;
            }
            
            // Clean and lowercase
            token = token.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            
            if (token.isEmpty() || stopStemService.isStopWord(token)) {
                continue;
            }
            
            String stemmed = stopStemService.stem(token);
            if (!stemmed.isEmpty()) {
                result.add(stemmed);
            }
        }
        
        return result;
    }
    
    /**
     * Extracts quoted phrases from a query
     */
    private List<String> extractQuotedPhrases(String userQuery) {
        List<String> quotedWords = new ArrayList<>();

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return quotedWords;
        }

        // Regex to match quoted phrases
        Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(userQuery);

        while (matcher.find()) {
            String[] words = matcher.group(1).trim().split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    quotedWords.add(word.toLowerCase());
                }
            }
        }

        return quotedWords;
    }
}