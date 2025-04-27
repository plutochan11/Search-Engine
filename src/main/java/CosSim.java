import jdbm.htree.HTree;

import java.io.IOException;
import java.util.*;
import java.lang.Math;

public class CosSim {

    /**
     * Calculates the term weights for the given query.
     *
     * @param query             List of tokens in the search query.
     * @param termToTermId      Mapping of term to TermInfo (containing termID and df).
     * @param bodyIndex List of inverted index records.
     * @param documentSize       Size of all document IDs (used for determining N).
     */
    public static void calculateCosSim(
            List<String> query,
            Map<String, TermInfo> termToTermId,
            HTree bodyIndex,
            Integer documentSize) {

        //weight of the query
        int L = query.size();
        if (L == 0) {
            System.out.println("Query has no valid terms after stopword removal.");
            return;
        }

        // Create a 1 x L query vector Q and initialize with 1's.
        int[] Q = new int[L];
        Arrays.fill(Q, 1);

        // Total number of documents in the collection (N)
        int N = documentSize;

        // Create a similarity table: Map<docId, double[]> with one row per document.
        // Each double[] holds the term weight for each term in the filtered query.
        Map<Integer, double[]> similarityTable = new HashMap<>();
        for (int i = 1; i <= N; i++) {
            similarityTable.put(i, new double[L]);
        }

        // For each term in the filtered query, compute term weight for each document.
        for (int j = 0; j < query.size(); j++) {
            String term = query.get(j);

            // Retrieve term info (termID and document frequency) from termToTermId.
            if (!termToTermId.containsKey(term)) {
                continue; // Skip term if not in the index.
            }
            TermInfo termInfo = termToTermId.get(term);
            int termID = termInfo.termId;
            int df = termInfo.frequency;
            if (df == 0) {
                continue; // Guard against division by zero.
            }

            // Calculate the inverse document frequency using log base 2.
            double idf = Math.log((double) N / df) / Math.log(2);

            try {
                // Iterate over all records in the inverted index.
                List<Posting> postings = (List<Posting>) bodyIndex.get(term);

                for (Posting posting : postings) {
                    // Process only records that belong to the current term.
                    double weight = posting.freq * idf;

                    // Store the computed weight in the similarity table in the appropriate column.
                    double[] weights = similarityTable.get(posting.doc);
                    weights[j] = weight;
                }
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        //calculate the cosine similarity (N*1) of similarityTable (N*M) and Q (M*1)
        //rank the docID based on cosine similarity from large to small
        Map<Integer, Double> similarityScores = new HashMap<>();

        for (Map.Entry<Integer, double[]> entry : similarityTable.entrySet()) {
            int docId = entry.getKey();
            double[] docVector = entry.getValue();

            // Compute dot product: Q ⋅ docVector
            double dotProduct = 0;
            double docNorm = 0;
            double queryNorm = 0;

            for (int i = 0; i < L; i++) {
                dotProduct += Q[i] * docVector[i];
                docNorm += docVector[i] * docVector[i];
                queryNorm += Q[i] * Q[i];
            }

            // Compute cosine similarity
            double cosineSimilarity = (docNorm == 0) ? 0 : dotProduct / (Math.sqrt(docNorm) * Math.sqrt(queryNorm));
            similarityScores.put(docId, cosineSimilarity);
        }


        // Output the filtered query, query vector, and similarity weights.
        System.out.print("Query Vector Q: ");
        for (int value : Q) {
            System.out.print(value + " ");
        }
        System.out.println();
        System.out.println("Similarity Table:");
        for (Integer docId : similarityTable.keySet()) {
            System.out.print("Document " + docId + ": ");
            double[] weights = similarityTable.get(docId);
            for (double w : weights) {
                System.out.print(w + " ");
            }
            System.out.println();
        }

        // Rank documents by cosine similarity (higher scores = higher relevance)
        List<Map.Entry<Integer, Double>> rankedDocs = new ArrayList<>(similarityScores.entrySet());
        rankedDocs.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); // Sort descending

        // Output ranked document results
        System.out.println("\nRanked Documents by Cosine Similarity:");
        for (Map.Entry<Integer, Double> entry : rankedDocs) {
            System.out.printf("Document %d -> Score: %.5f%n", entry.getKey(), entry.getValue());
        }
    }


}
