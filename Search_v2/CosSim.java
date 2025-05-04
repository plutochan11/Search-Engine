import jdbm.htree.HTree;

import java.io.IOException;
import java.util.*;
import java.lang.Math;
import java.util.stream.Collectors;

class TermData {
    private String term;
    private int frequency; // Combined frequency from title + body
    private List<Integer> titlePositions;
    private List<Integer> bodyPositions;

    public TermData(String term, int frequency, List<Integer> titlePositions, List<Integer> bodyPositions) {
        this.term = term;
        this.frequency = frequency;
        this.titlePositions = (titlePositions != null) ? titlePositions : new ArrayList<>();
        this.bodyPositions = (bodyPositions != null) ? bodyPositions : new ArrayList<>();
    }

    // Getters
    public String getTerm() { return term; }
    public int getFrequency() { return frequency; }
    public List<Integer> getTitlePositions() { return titlePositions; }
    public List<Integer> getBodyPositions() { return bodyPositions; }

    @Override
    public String toString() {
        return String.format("Term: %s, Frequency: %d, Title Positions: %s, Body Positions: %s",
                term, frequency, titlePositions, bodyPositions);
    }
}

public class CosSim {

    /**
     * Calculates term weights for the given query.
     *
     * @param query List of tokens in the search query.
     * @param termToTermId Mapping of term to TermInfo (containing termID and df).
     * @param titleIndex Title inverted index.
     * @param bodyIndex Body inverted index.
     * @param documentSize Number of documents.
     */
    public static Map<Integer, Object[]> calculateCosSim(
            List<String> query,
            Map<String, TermInfo> termToTermId,
            HTree titleIndex,
            HTree bodyIndex,
            Integer documentSize) {

        int L = query.size();
        if (L == 0) {
            System.out.println("Query has no valid terms after stopword removal.");
            return Collections.emptyMap();
        }

        int[] Q = new int[L]; // Query vector
        Arrays.fill(Q, 1);
        int N = documentSize;

        Map<Integer, double[]> titleSimilarityTable = new HashMap<>();
        Map<Integer, double[]> bodySimilarityTable = new HashMap<>();
        Map<Integer, Map<String, TermData>> queryTermDataTitle = new HashMap<>();
        Map<Integer, Map<String, TermData>> queryTermDataBody = new HashMap<>();

        for (int i = 1; i <= N; i++) {
            titleSimilarityTable.put(i, new double[L]);
            bodySimilarityTable.put(i, new double[L]);
            queryTermDataTitle.put(i, new HashMap<>());
            queryTermDataBody.put(i, new HashMap<>());
        }

        processIndex(query, termToTermId, titleIndex, titleSimilarityTable, queryTermDataTitle, N);
        processIndex(query, termToTermId, bodyIndex, bodySimilarityTable, queryTermDataBody, N);

        // Compute cosine similarity for each document
        Map<Integer, Object[]> similarityScores = new HashMap<>();
        for (int docId = 1; docId <= N; docId++) {
            double titleCosSim = computeCosineSimilarity(Q, titleSimilarityTable.get(docId));
            double bodyCosSim = computeCosineSimilarity(Q, bodySimilarityTable.get(docId));

            // Weighted final score: 60% from title, 40% from body
            double finalScore = (0.6 * titleCosSim) + (0.4 * bodyCosSim);

            // Merge term data while keeping separate positions
            Map<String, TermData> mergedTermData = mergeTermData(queryTermDataTitle.get(docId), queryTermDataBody.get(docId));

            List<TermData> topTermsWithDetails = getTop5TermsWithDetails(mergedTermData);

            similarityScores.put(docId, new Object[]{finalScore, topTermsWithDetails});

        }

        return similarityScores;
    }

    private static void processIndex(
            List<String> query,
            Map<String, TermInfo> termToTermId,
            HTree index,
            Map<Integer, double[]> similarityTable,
            Map<Integer, Map<String, TermData>> queryTermData,
            int N) {

        for (int j = 0; j < query.size(); j++) {
            String term = query.get(j);

            if (!termToTermId.containsKey(term)) continue;
            TermInfo termInfo = termToTermId.get(term);
            int df = termInfo.frequency;

            if (df == 0) continue;

            double idf = Math.log((double) N / df) / Math.log(2);

            try {
                List<Posting> postings = (List<Posting>) index.get(term);
                if (postings == null || postings.isEmpty()) continue;

                int maxTF = getMaxFrequency(postings);
                for (Posting posting : postings) {
                    double weight = posting.freq * idf / maxTF;
                    similarityTable.get(posting.doc)[j] = weight;

                    queryTermData.get(posting.doc).put(
                            term,
                            new TermData(term, posting.freq, posting.position, new ArrayList<>())
                    );
                }
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    public static int getMaxFrequency(List<Posting> postings) {
        if (postings == null || postings.isEmpty()) return 1;
        return postings.stream().mapToInt(posting -> posting.freq).max().orElse(1);
    }

    private static double computeCosineSimilarity(int[] Q, double[] docVector) {
        double dotProduct = 0, docNorm = 0, queryNorm = 0;

        for (int i = 0; i < Q.length; i++) {
            dotProduct += Q[i] * docVector[i];
            docNorm += docVector[i] * docVector[i];
            queryNorm += Q[i] * Q[i];
        }

        return (docNorm == 0) ? 0 : dotProduct / (Math.sqrt(docNorm) * Math.sqrt(queryNorm));
    }

    private static Map<String, TermData> mergeTermData(Map<String, TermData> titleData, Map<String, TermData> bodyData) {
        Map<String, TermData> mergedData = new HashMap<>();

        for (String term : titleData.keySet()) {
            TermData titleTermData = titleData.get(term);
            TermData bodyTermData = bodyData.get(term);
            int titleFreq = titleTermData != null ? titleTermData.getFrequency() : 0;
            int bodyFreq = bodyTermData != null ? bodyTermData.getFrequency() : 0;
            int totalFreq = titleFreq + bodyFreq;
            mergedData.put(term, new TermData(term, totalFreq, titleTermData.getTitlePositions(), (bodyTermData != null) ? bodyTermData.getTitlePositions() : new ArrayList<>()));
        }

        for (String term : bodyData.keySet()) {
            if (!mergedData.containsKey(term)) {
                TermData bodyTermData = bodyData.get(term);
                mergedData.put(term, new TermData(term, bodyTermData.getFrequency(), new ArrayList<>(), bodyTermData.getTitlePositions()));
            }
        }


        return mergedData;
    }

    public static List<TermData> getTop5TermsWithDetails(Map<String, TermData> queryTermData) {
        return queryTermData.values().stream()
                .sorted((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()))
                .limit(5)
                .collect(Collectors.toList());
    }
}

