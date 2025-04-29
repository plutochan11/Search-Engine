package hk.ust.csit5930.utils;
import jdbm.htree.HTree;

import java.util.*;

import hk.ust.csit5930.models.TermInfo;

public class SearchEngine {
    private StopStem stopStem;
    private Map<String, TermInfo> termToTermId;
    private HTree bodyIndex;
    private Map<Integer, Double> pageRankScores;
    private int documentSize;

    public SearchEngine(StopStem stopStem, Map<String, TermInfo> termToTermId, HTree bodyInvertedIndex, Map<Integer, Double> pageRankScores, int documentSize) {
        this.stopStem = stopStem;
        this.termToTermId = termToTermId;
        this.bodyIndex = bodyInvertedIndex;
        this.pageRankScores = pageRankScores;
        this.documentSize = documentSize;
    }

    public Map<String, Object> search(String userQuery) {
        // Check for empty or blank queries
        if (userQuery == null || userQuery.trim().isEmpty()) {
            System.out.println("Empty query detected. Please enter a valid search term.");
            return Collections.emptyMap();
        }
        
        // Process query: tokenize, remove stopwords, and stem
        Vector<String> queryVector = new Vector<>(Arrays.asList(userQuery.split(" ")));
        List<String> filterQuery = new ArrayList<>(TextProcessor.processWords(queryVector, stopStem).keySet());
        
        // Check if any valid terms remained after processing
        if (filterQuery.isEmpty()) {
            System.out.println("No valid search terms found after processing. Please try a different query.");
            return Collections.emptyMap();
        }

        // Compute cosine similarity scores (Includes positions)
        Map<Integer, Object[]> cosineScores = CosSim.calculateCosSim(filterQuery, termToTermId, bodyIndex, documentSize);
        // REMOVE documents with zero CosSim score
        cosineScores.entrySet().removeIf(entry -> (double) entry.getValue()[0] == 0.0);
        // **Check if there are no matching documents**
        if (cosineScores.isEmpty()) {
            System.out.println("No relevant documents found for your search query.");
            return Collections.emptyMap(); // Exit early with an empty result
        }

        // Compute combined scores (CosSim * PageRank)
        Map<Integer, Object[]> combinedScores = new HashMap<>();
        for (Integer docId : cosineScores.keySet()) {
            Object[] cosSimData = cosineScores.get(docId);
            double cosSimScore = (double) cosSimData[0]; // Extract CosSim score
            List<Integer> termPositions = (List<Integer>) cosSimData[1]; // Extract term positions
            double pageRankScore = pageRankScores.getOrDefault(docId, 0.0);
            double combinedScore = cosSimScore * pageRankScore;

            combinedScores.put(docId, new Object[]{cosSimScore, pageRankScore, combinedScore, termPositions});
        }

        // Sort documents for both rankings
        List<Map.Entry<Integer, Object[]>> cosineRanked = new ArrayList<>(cosineScores.entrySet());
        List<Map.Entry<Integer, Object[]>> combinedRanked = new ArrayList<>(combinedScores.entrySet());

        cosineRanked.sort((a, b) -> Double.compare((double) b.getValue()[0], (double) a.getValue()[0])); // Sort by CosSim descending
        combinedRanked.sort((a, b) -> Double.compare((double) b.getValue()[2], (double) a.getValue()[2])); // Sort by Combined Score descending

        // Store results correctly
        Map<String, Object> results = new HashMap<>();
        results.put("cosine", cosineRanked.subList(0, Math.min(10, cosineRanked.size()))); // CosSim ranking
        results.put("combined", combinedRanked.subList(0, Math.min(10, combinedRanked.size()))); // Combined ranking

        return results; // Return both rankings including term positions
    }
}
