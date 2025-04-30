import jdbm.htree.HTree;

import java.util.*;

import jdbm.htree.HTree;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchEngine {
    private StopStem stopStem;
    private Map<String, TermInfo> termToTermId;
    private HTree titleIndex;
    private HTree bodyIndex;
    private Map<Integer, Double> pageRankScores;
    private int documentSize;
    private Map<Integer, List<Integer>> indexedDocs;

    public SearchEngine(StopStem stopStem, Map<String, TermInfo> termToTermId, HTree titleInvertedIndex, HTree bodyInvertedIndex, Map<Integer, Double> pageRankScores, int documentSize,Map<Integer, List<Integer>> indexedDocs ) {
        this.stopStem = stopStem;
        this.termToTermId = termToTermId;
        this.titleIndex = titleInvertedIndex;
        this.bodyIndex = bodyInvertedIndex;
        this.pageRankScores = pageRankScores;
        this.documentSize = documentSize;
        this.indexedDocs = indexedDocs;
    }

    /**
     * Computes cosine similarity ranking.
     */
    public Map<Integer, Object[]> searchCosSim(String userQuery, Crawler crawler) {
        // Step 1: Extract quoted phrases before preprocessing
        List<String> quotedPhrases = extractQuotedPhrases(userQuery);
        System.out.println("Quoted Phrases: " + quotedPhrases);

        // Step 2: Process the rest of the query separately
        List<String> filterQuery = preprocessQuery(userQuery);
        Map<Integer, Object[]> cosineScores = CosSim.calculateCosSim(filterQuery, termToTermId, titleIndex, bodyIndex, documentSize);


        // Remove entries with zero final cosine similarity
        cosineScores.entrySet().removeIf(entry -> {
            Object value = entry.getValue()[0];
            return value instanceof Double && (double) value == 0.0;
        });

        if (cosineScores.isEmpty()) {
            System.out.println("No relevant documents found.");
            return Collections.emptyMap();
        }

        Map<Integer, Object[]> formattedResults = new HashMap<>();
        for (Map.Entry<Integer, Object[]> entry : cosineScores.entrySet()) {
            int docId = entry.getKey();
            Object[] data = entry.getValue();

            // Extract separate similarity scores
            double finalCosSimScore = (double) data[0]; // Weighted 60% title, 40% body

            List<TermData> topTermsWithDetails = (List<TermData>) data[1]; // Extract top term details

            // Convert `TermData` to term frequency mappings while keeping title/body positions separate
            List<Map.Entry<String, Integer>> topTermsWithFrequency = new ArrayList<>();
            Map<String, List<Integer>> titleTermPositions = new HashMap<>();
            Map<String, List<Integer>> bodyTermPositions = new HashMap<>();
            Map<Integer, List<String>> termUrls = new HashMap<>();

            String title = crawler.getUrlFromDocId(docId)[1];
            String url = crawler.getUrlFromDocId(docId)[0];

            for (TermData termData : topTermsWithDetails) {
                topTermsWithFrequency.add(Map.entry(termData.getTerm(), termData.getFrequency())); // Store term-frequency pairs
                titleTermPositions.put(termData.getTerm(), termData.getTitlePositions()); // Store title positions separately
                bodyTermPositions.put(termData.getTerm(), termData.getBodyPositions()); // Store body positions separately
            }

            // Fetch child URLs safely
            List<Integer> childDocIds = indexedDocs.get(docId);
            if (childDocIds == null || childDocIds.isEmpty()) {
                termUrls.put(docId, List.of("No child URLs found."));
            } else {
                for (int childDocId : childDocIds) {
                    String childUrl = crawler.getUrlFromDocId(childDocId)[0];
                    termUrls.computeIfAbsent(docId, k -> new ArrayList<>()).add(childUrl);
                }
            }

            //get the surrounding words
            Vector<String> bodyWords = crawler.getWordFromDocId(docId);
            List<String> snapWords = getCombinedSnippets(bodyTermPositions, bodyWords);

            // Step 3: Check if the full phrase exists **in either the title OR body**
            boolean fullPhraseExistsInTitle = checkPhraseSequence(titleTermPositions, quotedPhrases);
            boolean fullPhraseExistsInBody = checkPhraseSequence(bodyTermPositions, quotedPhrases);
            // Step 4: Add to results if **either title OR body contains the full phrase**
            if (fullPhraseExistsInTitle || fullPhraseExistsInBody) {
                formattedResults.put(docId, new Object[]{
                        title,
                        url,
                        finalCosSimScore, // Weighted score
                        topTermsWithFrequency, // Top terms
                        titleTermPositions, // Title term positions
                        bodyTermPositions, // Body term positions
                        termUrls, // URLs
                        snapWords //snap sentence
                });
            }
        }

        return formattedResults.entrySet().stream()
                .sorted((a, b) -> Double.compare((double) b.getValue()[2], (double) a.getValue()[2])) // Sort by highest combined score
                .limit(50) // Keep top 50 results
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Computes combined ranking using cosine similarity * PageRank.
     */
    public Map<Integer, Object[]> searchCombined(String userQuery, Crawler crawler) {
        Map<Integer, Object[]> cosineScores = searchCosSim(userQuery, crawler);
        if (cosineScores.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Object[]> combinedScores = new HashMap<>();

        for (Map.Entry<Integer, Object[]> entry : cosineScores.entrySet()) {
            Integer docId = entry.getKey();
            Object[] cosSimData = entry.getValue();

            String title = (String) cosSimData[0];
            String url = (String) cosSimData[1];
            double finalCosSimScore = (double) cosSimData[2]; // Weighted cosine score (60% title, 40% body)

            // Extract term frequency list safely
            List<Map.Entry<String, Integer>> topTermsWithFrequency = (List<Map.Entry<String, Integer>>) cosSimData[3];

            // Extract term positions safely
            Map<String, List<Integer>> titleTermPositions = (Map<String, List<Integer>>) cosSimData[4]; // Title term positions
            Map<String, List<Integer>> bodyTermPositions = (Map<String, List<Integer>>) cosSimData[5]; // Body term positions

            // Extract child URLs safely
            Map<Integer, List<String>> termUrls = (Map<Integer, List<String>>) cosSimData[6];

            //get the surrounding words
            List<String> snapWords = (List<String>) cosSimData[7];

            // Fetch child URLs safely
            String[] urls = crawler.getUrlFromDocId(docId);
            if (urls == null || urls.length == 0) {
                urls = new String[]{"No child URLs found."}; // Default message if empty
            }

            // Compute combined score using PageRank
            double pageRankScore = pageRankScores.getOrDefault(docId, 0.0);
            double combinedScore = finalCosSimScore * pageRankScore;

            // Store formatted results
            combinedScores.put(docId, new Object[]{
                    title, url, combinedScore,
                    topTermsWithFrequency, titleTermPositions, bodyTermPositions, termUrls, snapWords
            });
        }

        // Sort documents in descending order & get top 50
        return combinedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare((double) b.getValue()[2], (double) a.getValue()[2])) // Sort by highest combined score
                .limit(50) // Keep top 50 results
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    /**
     * Preprocesses the user query (tokenization, stopword removal, stemming).
     */
    private List<String> preprocessQuery(String userQuery) {
        Vector<String> queryVector = new Vector<>(Arrays.asList(userQuery.split(" ")));
        return new ArrayList<>(Main.processWords(queryVector, stopStem).keySet());
    }
    /**
     * Extracts AND splits all quoted phrases into individual words,
     * while also preserving non-quoted terms
     */
    public List<String> extractQuotedPhrases(String userQuery) {
        List<String> quotedWords = new ArrayList<>();

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return quotedWords;
        }

        // Regex to match ONLY quoted phrases (e.g., "Hong Kong")
        Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(userQuery);

        while (matcher.find()) {
            // Split the quoted phrase into individual words
            String[] words = matcher.group(1).trim().split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    quotedWords.add(word.toLowerCase()); // Store in lowercase
                }
            }
        }

        return quotedWords;
    }

    /**
     * Helper function to check if a quoted phrase sequence exists in term positions.
     */
    private static boolean checkPhraseSequence(Map<String, List<Integer>> termPositions,
                                               List<String> quotedPhrases) {
        if (quotedPhrases.isEmpty()) {
            return true;
        }

        for (String term : quotedPhrases) {
            if (!termPositions.containsKey(term)) {
                return false;
            }
        }
        // Preprocess positions into HashSet for O(1) lookups
        Map<String, Set<Integer>> positionMap = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : termPositions.entrySet()) {
            positionMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        String firstTerm = quotedPhrases.get(0);
        List<Integer> firstTermPositions = termPositions.get(firstTerm);

        // Check each potential sequence
        for (int startPos : firstTermPositions) {
            boolean fullMatch = true;
            int currentPos = startPos;

            // Verify consecutive terms
            for (int i = 1; i < quotedPhrases.size(); i++) {
                currentPos++;
                String currentTerm = quotedPhrases.get(i);
                Set<Integer> validPositions = positionMap.get(currentTerm);

                if (!validPositions.contains(currentPos)) {
                    fullMatch = false;
                    break;
                }
            }

            if (fullMatch) {
                return true;
            }
        }

        return false;
    }

    public List<String> getCombinedSnippets(Map<String, List<Integer>> termPositions,
                                            List<String> textTokens) {
        List<String> combinedSnippets = new ArrayList<>();
        List<int[]> snippetRanges = new ArrayList<>();

        // 1. Generate merged snippet ranges
        termPositions.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .sorted(Comparator.comparingInt(entry -> entry.getValue().get(0)))
                .forEach(entry -> {
                    int pos = entry.getValue().get(0);
                    int start = Math.max(0, pos - 4);
                    int end = Math.min(textTokens.size() - 1, pos + 4);

                    // Check for overlaps with existing ranges
                    boolean merged = false;
                    for (int[] range : snippetRanges) {
                        if (start <= range[1] && end >= range[0]) {
                            range[0] = Math.min(range[0], start);
                            range[1] = Math.max(range[1], end);
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        snippetRanges.add(new int[]{start, end});
                    }
                });

        // 2. Build combined snippets
        for (int[] range : snippetRanges) {
            StringBuilder snippet = new StringBuilder();
            Set<String> keywordsInRange = new HashSet<>();

            // Collect all keywords in this range
            termPositions.forEach((term, positions) -> {
                if (!positions.isEmpty() && positions.get(0) >= range[0]
                        && positions.get(0) <= range[1]) {
                    keywordsInRange.add(term);
                }
            });

            // Build the snippet text
            for (int i = range[0]; i <= range[1]; i++) {
                snippet.append(textTokens.get(i)).append(" ");
            }

            // Add metadata to snippet
            String snippetWithKeywords = String.format("[Keywords: %s] %s",
                    String.join(", ", keywordsInRange),
                    snippet.toString().trim());

            combinedSnippets.add(snippetWithKeywords);
        }

        return combinedSnippets;
    }

}


