package hk.ust.csit5930;

import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import org.htmlparser.util.ParserException;

import hk.ust.csit5930.models.TermInfo;
import hk.ust.csit5930.models.WordInfo;
import hk.ust.csit5930.utils.InvertedIndex;
import hk.ust.csit5930.utils.PageRank;
import hk.ust.csit5930.utils.SearchEngine;
import hk.ust.csit5930.utils.StopStem;
import hk.ust.csit5930.utils.TextProcessor;

import java.io.IOException;
import java.util.*;

/**
 * Main application class for the search engine
 */
public class App {
    // Constants
    private static final String ROOT_URL = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
    private static final String STOPWORDS_PATH = "search-engine/src/main/resources/stopwords.txt";
    private static final String BODY_INDEX_DB = "recordmanager2";
    private static final String BODY_INDEX_NAME = "bodyIndex";
    private static final int PAGERANK_ITERATIONS = 5;
    private static final double PAGERANK_DAMPING_FACTOR = 0.8;
    
    public static void main(String[] args) {
        try {
            // Initialize components
            Spider crawler = initCrawler();
            StopStem stopStem = new StopStem(STOPWORDS_PATH);
            InvertedIndex bodyInvertedIndex = new InvertedIndex(BODY_INDEX_DB, BODY_INDEX_NAME);

            // Step 1: Crawl web pages
            Map<Integer, List<Integer>> indexedDocs = crawlWebPages(crawler);
            int[][] linkMatrix = crawler.getLinkMatrix();

            // Step 2: Build indexes
            Map<String, TermInfo> termToTermId = new HashMap<>();
            Map<Integer, String> termIdToTerm = new HashMap<>();
            Map<Integer, List<Integer>> docTermIndex = new HashMap<>();
            
            buildIndexes(crawler, stopStem, bodyInvertedIndex, indexedDocs, 
                         termToTermId, termIdToTerm, docTermIndex);

            // Step 3: Compute PageRank
            Map<Integer, Double> pageRankScores = computePageRank(linkMatrix);

            // Step 4: Initialize search engine and handle queries
            runSearchEngine(stopStem, termToTermId, bodyInvertedIndex, pageRankScores, 
                           indexedDocs.size(), crawler);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize and configure the web crawler
     */
    private static Spider initCrawler() {
        Spider crawler = new Spider();
        crawler.fromScratch(true);
        return crawler;
    }

    /**
     * Crawl web pages starting from the root URL
     */
    private static Map<Integer, List<Integer>> crawlWebPages(Spider crawler) {
        crawler.crawl();
        Map<Integer, List<Integer>> indexedDocs = crawler.getRelationships();
        return indexedDocs;
    }

    /**
     * Build term indexes and inverted indexes for search
     * @throws IOException 
     */
    private static void buildIndexes(Spider crawler, StopStem stopStem, 
                                   InvertedIndex bodyInvertedIndex,
                                   Map<Integer, List<Integer>> indexedDocs,
                                   Map<String, TermInfo> termToTermId, 
                                   Map<Integer, String> termIdToTerm,
                                   Map<Integer, List<Integer>> docTermIndex) throws IOException {
        System.out.println("Indexing documents...");
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
    private static Map<Integer, Double> computePageRank(int[][] linkMatrix) {
        PageRank pageRank = new PageRank(linkMatrix);
        pageRank.computePageRank(PAGERANK_ITERATIONS, PAGERANK_DAMPING_FACTOR);
        return pageRank.getPageRankScores();
    }

    /**
     * Run the search engine and handle user queries
     */
    private static void runSearchEngine(StopStem stopStem, Map<String, TermInfo> termToTermId, 
                                      InvertedIndex bodyInvertedIndex, 
                                      Map<Integer, Double> pageRankScores, 
                                      int totalDocs, Spider crawler) {
        // Initialize SearchEngine
        SearchEngine searchEngine = new SearchEngine(stopStem, termToTermId, 
                                                     bodyInvertedIndex.getHashtable(), 
                                                     pageRankScores, totalDocs);
        
        // Create a scanner for user input
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\nEnter your search query (or type 'exit' to quit): ");
            String userQuery = scanner.nextLine();
            long searchStartTime = System.currentTimeMillis();
            
            // Check if user wants to exit
            if (userQuery.equalsIgnoreCase("exit")) {
                System.out.println("Exiting search engine. Goodbye!");
                break; // Exit the loop
            }
            
            // Perform search and retrieve results
            Map<String, Object> results = searchEngine.search(userQuery);
            
            displaySearchResults(results, crawler);
            
            long searchEndTime = System.currentTimeMillis();
            System.out.println("Searching Time: " + (searchEndTime - searchStartTime)/1000.0);
        }
        
        scanner.close(); // Close scanner when finished
    }

    /**
     * Display search results to the user
     */
    private static void displaySearchResults(Map<String, Object> results, Spider crawler) {
        // Check if search results are empty
        if (results.isEmpty()) {
            System.out.println("No relevant documents found for your search query.");
            return;
        } 
        
        // Print cosine similarity results
        System.out.println("\nTop 10 Results (Based on Cosine Similarity):");
        List<Map.Entry<Integer, Object[]>> cosineResults = (List<Map.Entry<Integer, Object[]>>) results.get("cosine");
        displayResultSet(cosineResults, crawler, false);

        // Print combined score results
        System.out.println("\nTop 10 Results (Based on Combined Score - CosSim * PageRank):");
        List<Map.Entry<Integer, Object[]>> combinedResults = (List<Map.Entry<Integer, Object[]>>) results.get("combined");
        displayResultSet(combinedResults, crawler, true);
    }
    
    /**
     * Display a specific set of results (either cosine or combined)
     */
    private static void displayResultSet(List<Map.Entry<Integer, Object[]>> results, 
                                       Spider crawler, boolean isCombined) {
        for (Map.Entry<Integer, Object[]> entry : results) {
            int docId = entry.getKey();
            Object[] data = entry.getValue();
            double cosSimScore = (double) data[0];
            
            String url = crawler.getUrl(docId);
            String title = crawler.getTitle(docId);
            Vector<String> content = crawler.getContent(docId);
            
            if (isCombined) {
                double pageRankScore = (double) data[1];
                double combinedScore = (double) data[2];
                List<Integer> positions = (List<Integer>) data[3];
                
                System.out.printf("DocID: %d | URL: %s | CosSim: %.5f | PageRank: %.5f | Combined Score: %.5f%n",
                        docId, url, cosSimScore, pageRankScore, combinedScore);
                
                System.out.println("Title: " + title);
                displaySnippets(content, positions);
            } else {
                List<Integer> positions = (List<Integer>) data[1];
                
                System.out.printf("DocID: %d | URL: %s | CosSim: %.5f%n", docId, url, cosSimScore);
                System.out.println("Title: " + title);
                displaySnippets(content, positions);
            }
        }
    }
    
    /**
     * Display text snippets showing search terms in context
     */
    private static void displaySnippets(Vector<String> content, List<Integer> positions) {
        StringBuilder fullSnippet = new StringBuilder();
        for (int pos : positions) {
            String contextSnippet = TextProcessor.getSurroundingWords(content, pos);
            fullSnippet.append(contextSnippet).append(" ... ");
        }
        System.out.println("Context: " + fullSnippet.toString().trim());
    }
}
