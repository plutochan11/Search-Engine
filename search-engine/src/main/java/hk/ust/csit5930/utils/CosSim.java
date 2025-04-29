package hk.ust.csit5930.utils;
import jdbm.htree.HTree;

import java.io.IOException;
import java.util.*;

import hk.ust.csit5930.models.TermInfo;

import java.lang.Math;

public class CosSim {

    /**
     * Calculates the term weights for the given query.
     *
     * @param query        List of tokens in the search query.
     * @param termToTermId Mapping of term to TermInfo (containing termID and df).
     * @param bodyIndex    List of inverted index records.
     * @param documentSize Size of all document IDs (used for determining N).
     */
    public static Map<Integer, Object[]> calculateCosSim(
            List<String> query,
            Map<String, TermInfo> termToTermId,
            HTree bodyIndex,
            Integer documentSize) {

        // Weight of the query
        int L = query.size();
        if (L == 0) {
            System.out.println("Query has no valid terms after stopword removal.");
            return Collections.emptyMap();  // Return empty map instead of null
        }

        // Create a 1 x L query vector Q and initialize with 1's.
        int[] Q = new int[L];
        Arrays.fill(Q, 1);

        // Total number of documents in the collection (N)
        int N = documentSize;
        if (N <= 0) {
            System.out.println("No documents in collection.");
            return Collections.emptyMap();
        }

        // Create a similarity table: Map<docId, double[]> (term weights per document)
        Map<Integer, double[]> similarityTable = new HashMap<>();
        Map<Integer, List<Integer>> termPositions = new HashMap<>(); // Store positions of query terms

        for (int i = 1; i <= N; i++) {
            similarityTable.put(i, new double[L]);
            termPositions.put(i, new ArrayList<>());
        }

        // For each term in the query, compute term weight & track term positions
        for (int j = 0; j < query.size(); j++) {
            String term = query.get(j);

            // Retrieve term info (termID and document frequency)
            if (!termToTermId.containsKey(term)) {
                continue; // Skip term if not in index
            }
            TermInfo termInfo = termToTermId.get(term);
            int df = termInfo.frequency;
            if (df == 0) {
                continue; // Guard against division by zero
            }

            // Compute inverse document frequency (IDF)
            double idf = Math.log((double) N / df) / Math.log(2);

            try {
                // Iterate over all records in the inverted index
                List<Posting> postings = (List<Posting>) bodyIndex.get(term);
                
                // Add null check for postings
                if (postings == null || postings.isEmpty()) {
                    continue;
                }
                
                int maxtf = getMaxFrequency(postings);
                if (maxtf == 0) {
                    continue; // Avoid division by zero
                }
                
                for (Posting posting : postings) {
                    // Add null check for posting
                    if (posting == null) {
                        continue;
                    }
                    
                    double weight = posting.freq * idf / maxtf;

                    // Store term weight in similarity table - with null check
                    double[] weights = similarityTable.get(posting.doc);
                    if (weights == null) {
                        // This document ID wasn't pre-initialized - create a new weights array
                        weights = new double[L];
                        similarityTable.put(posting.doc, weights);
                    }
                    
                    // Make sure j is within bounds of the weights array
                    if (j < weights.length) {
                        weights[j] = weight;
                    }

                    // Store term positions in document - with null check
                    List<Integer> positions = termPositions.get(posting.doc);
                    if (positions == null) {
                        positions = new ArrayList<>();
                        termPositions.put(posting.doc, positions);
                    }
                    
                    if (posting.position != null) {
                        positions.addAll(posting.position);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        // Compute cosine similarity & rank documents
        Map<Integer, Object[]> similarityScores = new HashMap<>();

        for (Map.Entry<Integer, double[]> entry : similarityTable.entrySet()) {
            int docId = entry.getKey();
            double[] docVector = entry.getValue();
            
            // Add null check for docVector
            if (docVector == null) {
                continue;
            }

            // Compute dot product: Q ⋅ docVector
            double dotProduct = 0;
            double docNorm = 0;
            double queryNorm = 0;

            for (int i = 0; i < L && i < docVector.length; i++) {
                dotProduct += Q[i] * docVector[i];
                docNorm += docVector[i] * docVector[i];
                queryNorm += Q[i] * Q[i];
            }

            // Compute cosine similarity
            double cosineSimilarity = (docNorm == 0 || queryNorm == 0) ? 0 : 
                                     dotProduct / (Math.sqrt(docNorm) * Math.sqrt(queryNorm));

            // Only include documents with non-zero similarity
            if (cosineSimilarity > 0) {
                // Store both cosine similarity & term positions
                List<Integer> positions = termPositions.getOrDefault(docId, Collections.emptyList());
                similarityScores.put(docId, new Object[]{cosineSimilarity, positions});
            }
        }

        return similarityScores; // Return results with positions included
    }

    public static int getMaxFrequency(List<Posting> postings) {
        if (postings == null || postings.isEmpty()) {
            return 0;
        }
        
        int maxFreq = 0; // Initialize with lowest possible value

        for (Posting posting : postings) {
            if (posting != null) {
                maxFreq = Math.max(maxFreq, posting.freq); // Update max if higher freq found
            }
        }

        return maxFreq;
    }
}
