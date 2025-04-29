package hk.ust.csit5930.service;

import hk.ust.csit5930.Spider;
import hk.ust.csit5930.config.SearchEngineConfig;
import hk.ust.csit5930.models.TermInfo;
import hk.ust.csit5930.models.WordInfo;
import hk.ust.csit5930.utils.InvertedIndex;
import hk.ust.csit5930.utils.PageRank;
import hk.ust.csit5930.utils.SearchEngine;
import hk.ust.csit5930.utils.StopStem;
import hk.ust.csit5930.utils.TextProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Service
public class SearchEngineService {
    private static final Logger logger = LoggerFactory.getLogger(SearchEngineService.class);

    private final SearchEngineConfig config;
    
    private Spider crawler;
    private StopStem stopStem;
    private InvertedIndex bodyInvertedIndex;
    private Map<String, TermInfo> termToTermId;
    private Map<Integer, String> termIdToTerm;
    private Map<Integer, Double> pageRankScores;
    private Map<Integer, List<Integer>> indexedDocs;
    private SearchEngine searchEngine;

    @Autowired
    public SearchEngineService(SearchEngineConfig config) {
        this.config = config;
    }

    /**
     * Initialize search engine components on application startup
     */
    @PostConstruct
    public void initializeSearchEngine() {
        logger.info("Initializing search engine...");
        
        try {
            // Initialize crawler and components
            crawler = initCrawler();
            stopStem = new StopStem(config.getStopwordsPath());
            bodyInvertedIndex = new InvertedIndex(config.getBodyIndexDb(), config.getBodyIndexName());
            termToTermId = new HashMap<>();
            termIdToTerm = new HashMap<>();
            
            // Start crawling and indexing
            logger.info("Starting crawler...");
            indexedDocs = crawlWebPages(crawler);
            int[][] linkMatrix = crawler.getLinkMatrix();
            
            // Build indexes
            logger.info("Building indexes...");
            Map<Integer, List<Integer>> docTermIndex = new HashMap<>();
            buildIndexes(crawler, stopStem, bodyInvertedIndex, indexedDocs, 
                         termToTermId, termIdToTerm, docTermIndex);
            
            // Compute PageRank
            logger.info("Computing PageRank scores...");
            pageRankScores = computePageRank(linkMatrix);
            
            // Initialize search engine
            searchEngine = new SearchEngine(stopStem, termToTermId, 
                                           bodyInvertedIndex.getHashtable(),
                                           pageRankScores, indexedDocs.size());
            
            logger.info("Search engine initialization complete. Ready to handle search requests.");
        } catch (IOException e) {
            logger.error("Failed to initialize search engine", e);
            throw new RuntimeException("Search engine initialization failed", e);
        }
    }

    /**
     * Search for documents matching the provided query
     * 
     * @param query the search query
     * @return search results containing document information and relevance scores
     */
    public Map<String, Object> search(String query) {
        logger.debug("Processing search query: {}", query);
        return searchEngine.search(query);
    }
    
    /**
     * Get all details for a specific document
     * 
     * @param docId the document ID
     * @return document details including title, URL, and content
     */
    public Map<String, Object> getDocumentDetails(int docId) {
        Map<String, Object> details = new HashMap<>();
        
        if (indexedDocs.containsKey(docId)) {
            details.put("docId", docId);
            details.put("title", crawler.getTitle(docId));
            details.put("url", crawler.getUrl(docId));
            details.put("content", crawler.getContent(docId));
            details.put("pageRank", pageRankScores.getOrDefault(docId, 0.0));
            
            List<Integer> linkedDocs = indexedDocs.get(docId);
            List<Map<String, Object>> outlinks = new ArrayList<>();
            
            for (Integer linkedDocId : linkedDocs) {
                if (crawler.getUrl(linkedDocId) != null) {
                    Map<String, Object> outlink = new HashMap<>();
                    outlink.put("docId", linkedDocId);
                    outlink.put("url", crawler.getUrl(linkedDocId));
                    outlink.put("title", crawler.getTitle(linkedDocId));
                    outlinks.add(outlink);
                }
            }
            
            details.put("outlinks", outlinks);
        } else {
            logger.warn("Document with ID {} not found", docId);
            details.put("error", "Document not found");
        }
        
        return details;
    }
    
    /**
     * Get a summary of all indexed documents
     * 
     * @return list of all documents with basic information
     */
    public List<Map<String, Object>> getAllDocuments() {
        List<Map<String, Object>> documents = new ArrayList<>();
        
        for (Integer docId : indexedDocs.keySet()) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("docId", docId);
            doc.put("title", crawler.getTitle(docId));
            doc.put("url", crawler.getUrl(docId));
            doc.put("pageRank", pageRankScores.getOrDefault(docId, 0.0));
            documents.add(doc);
        }
        
        // Sort by pageRank score (highest first)
        documents.sort((a, b) -> Double.compare((Double)b.get("pageRank"), (Double)a.get("pageRank")));
        
        return documents;
    }

    /**
     * Initialize and configure the web crawler
     */
    private Spider initCrawler() {
        Spider crawler = new Spider();
        crawler.fromScratch(true);
        return crawler;
    }

    /**
     * Crawl web pages starting from the root URL
     */
    private Map<Integer, List<Integer>> crawlWebPages(Spider crawler) {
        crawler.crawl();
        return crawler.getRelationships();
    }

    /**
     * Build term indexes and inverted indexes for search
     * @throws IOException 
     */
    private void buildIndexes(Spider crawler, StopStem stopStem, 
                            InvertedIndex bodyInvertedIndex,
                            Map<Integer, List<Integer>> indexedDocs,
                            Map<String, TermInfo> termToTermId, 
                            Map<Integer, String> termIdToTerm,
                            Map<Integer, List<Integer>> docTermIndex) throws IOException {
        int nextTermId = 1;

        for (Integer docId : indexedDocs.keySet()) {
            Vector<String> bodyWords = crawler.getContent(docId);
            Map<String, WordInfo> bodyWordFreq = TextProcessor.processWords(bodyWords, stopStem);

            for (Map.Entry<String, WordInfo> entry : bodyWordFreq.entrySet()) {
                String term = entry.getKey();
                WordInfo wordInfo = entry.getValue();

                // Add TermID into docTermIndex
                if (!docTermIndex.containsKey(docId)) {
                    docTermIndex.put(docId, new ArrayList<>());
                }
                List<Integer> termList = docTermIndex.get(docId);
                termList.add(nextTermId);

                // Assign TermID into termToTermId and termIdToTerm and update the document frequency
                if (!termToTermId.containsKey(term)) {
                    TermInfo termInfo = new TermInfo(term, nextTermId, 1);
                    termToTermId.put(term, termInfo);
                    termIdToTerm.put(nextTermId, term);
                    nextTermId++;
                } else {
                    TermInfo termInfo = termToTermId.get(term);
                    termInfo.setFrequency(termInfo.getFrequency() + 1);
                }

                // Update invertedFileIndex
                bodyInvertedIndex.addEntry(term, docId, wordInfo);
            }
        }
    }

    /**
     * Compute PageRank scores for all documents
     */
    private Map<Integer, Double> computePageRank(int[][] linkMatrix) {
        PageRank pageRank = new PageRank(linkMatrix);
        pageRank.computePageRank(config.getPagerankIterations(), config.getPagerankDampingFactor());
        return pageRank.getPageRankScores();
    }
}