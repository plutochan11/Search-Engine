
import org.htmlparser.util.ParserException;

import java.io.IOException;
import java.util.*;


public class Main {
    public static void main(String[] args) {
        long start_time = System.currentTimeMillis();
        String rootUrl = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"; // URL to crawl and index
        try {
            // Initialize the crawler
            Crawler crawler = new Crawler(rootUrl);
            StopStem stopStem = new StopStem("stopwords.txt");
            // Initialize the index
            InvertedIndex titleInvertedIndex = new InvertedIndex("recordmanager1", "titleIndex");
            InvertedIndex bodyInvertedIndex = new InvertedIndex("recordmanager2", "bodyIndex");

            // Step 1: Crawl all links and assign document IDs
            Map<Integer, List<Integer>> indexedDocs = crawler.crawlAllLinks();
            long crawlerEndTime = System.currentTimeMillis();
            System.out.println("Crawler Duration: " + (crawlerEndTime - start_time)/1000.0);

            // Retrieve the link matrix
            int[][] linkMatrix = crawler.getLinkMatrix();

            // Step 2: Process each page title & body separately
            // Initial index mapping
            Map<String, TermInfo> termToTermId = new HashMap<>(); //Term Index: term ->termID, document frequency
            Map<Integer, String> termIdToTerm = new HashMap<>(); //termID ->term
            Map<Integer, List<Integer>> docTermIndex = new HashMap<>(); //Forward Index: docID ->termID
            int nextTermId = 1;

            for (Integer docId : indexedDocs.keySet()) {
                // Step 1: Process the Title
                String title = crawler.getUrlFromDocId(docId)[1]; // Get the document title
                Vector<String> titleWords = tokenizeTitle(title); // Function to tokenize title into words
                Map<String, WordInfo> titleWordFreq = processWords(titleWords, stopStem);

                for (Map.Entry<String, WordInfo> entry : titleWordFreq.entrySet()) {
                    String term = entry.getKey();
                    WordInfo wordInfo = entry.getValue();

                    // Assign term ID for indexing
                    if (!termToTermId.containsKey(term)) {
                        TermInfo termInfo = new TermInfo(term, nextTermId, 1);
                        termToTermId.put(term, termInfo);
                        nextTermId++;
                    } else {
                        TermInfo termInfo = termToTermId.get(term);
                        termInfo.setFrequency(termInfo.getFrequency() + 1);
                    }

                    // Update inverted index for title
                    titleInvertedIndex.addEntry(term, docId, wordInfo);
                }

                // Step 2: Process the Body (Existing Code)
                Vector<String> bodyWords = crawler.getWordFromDocId(docId);
                Map<String,  WordInfo> bodyWordFreq = processWords(bodyWords, stopStem);

                for (Map.Entry<String, WordInfo> entry : bodyWordFreq.entrySet()) {
                    String term = entry.getKey();
                    WordInfo wordInfo = entry.getValue();

                    //add TermID into docTermIndex
                    if (!docTermIndex.containsKey(docId)) {
                        docTermIndex.put(docId, new ArrayList<>());
                    }
                    List<Integer> termList = docTermIndex.get(docId);
                    termList.add(nextTermId);

                    //assign TermID into termToTermId and termIdToTerm and update the document frequency
                    if (!termToTermId.containsKey(term)) {
                        TermInfo termInfo = new TermInfo(term, nextTermId, 1);
                        termToTermId.put(term, termInfo);
                        nextTermId++;
                    } else {
                        TermInfo termInfo = termToTermId.get(term);
                        termInfo.setFrequency(termInfo.getFrequency() + 1);
                    }


                    //update invertedFileIndex
                    bodyInvertedIndex.addEntry(term, docId, wordInfo); // Index for body
                }
            }

            long indexEndTime = System.currentTimeMillis();
            System.out.println("Indexing Duration: " + (indexEndTime - crawlerEndTime)/1000.0);

            //testing
            // Run PageRank using the matrix
            PageRank pageRank = new PageRank(linkMatrix);
            pageRank.computePageRank(5, 0.8);
            Map<Integer, Double> pageRankScores = pageRank.getPageRankScores();

            // Initialize Search Engine with titleIndex & bodyIndex
            SearchEngine searchEngine = new SearchEngine(stopStem, termToTermId,
                    titleInvertedIndex.getHashtable(), bodyInvertedIndex.getHashtable(),
                    pageRankScores, indexedDocs.size(), indexedDocs);

            // Create a scanner for user input
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("\nEnter your search query (or type 'exit' to quit): ");
                String userQuery = scanner.nextLine();
                long searchStartTime = System.currentTimeMillis();

                if (userQuery.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting search engine. Goodbye!");
                    break;
                }

                // Perform cosine similarity search
                Map<Integer, Object[]> cosineResults = searchEngine.searchCosSim(userQuery, crawler);


                if (cosineResults.isEmpty()) {
                    System.out.println("No relevant documents found.");
                } else {
                    System.out.println("\nTop 50 Results (Based on Cosine Similarity):");
                    printResults(cosineResults);
                }

                // Perform combined search (CosSim * PageRank)
                Map<Integer, Object[]> combinedResults = searchEngine.searchCombined(userQuery, crawler);
                if (!combinedResults.isEmpty()) {
                    System.out.println("\nTop 50 Results (Based on Combined Score - CosSim * PageRank):");
                    printResults(combinedResults);
                }

                long searchEndTime = System.currentTimeMillis();
                System.out.println("Searching Time: " + (searchEndTime - searchStartTime) / 1000.0);

            }

            scanner.close();
        } catch (IOException | ParserException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Helper function to print search results.
     */
    public static void printResults(Map<Integer, Object[]> results) {
        if (results.isEmpty()) {
            System.out.println("No relevant documents found.");
            return;
        }

        System.out.println("\n===== Search Results (Top 50 Ranked) =====\n");

        // Sort results in descending order based on the score (index 2 in Object[])
        List<Map.Entry<Integer, Object[]>> sortedResults = results.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare((double) b.getValue()[2], (double) a.getValue()[2])) // Descending order
                .limit(50) // Get top 50 results
                .toList();
        int i=1;
        for (Map.Entry<Integer, Object[]> entry : sortedResults) {
            Integer docId = entry.getKey();
            Object[] data = entry.getValue();

            // Extract necessary details
            String title = (String) data[0];
            String url = (String) data[1];
            double score = (double) data[2]; // Ranking Score

            List<Map.Entry<String, Integer>> topTermsWithFrequency = (List<Map.Entry<String, Integer>>) data[3];
            Map<String, List<Integer>> termPositions = (Map<String, List<Integer>>) data[5];
            Map<Integer, List<String>> termUrls = (Map<Integer, List<String>>) data[6];
            List<String> snapWords = (List<String>) data[7];

            // Print Title, URL, and Score
            System.out.println("**No: " + i);
            System.out.println("🔹 **Title:** " + title);
            System.out.println("🔗 **URL:** " + url);
            System.out.printf("⭐ **Score:** %.5f\n", score);

            // Print Top Query Terms & Frequencies
            System.out.println("📌 **Top Query Terms:**");
            for (Map.Entry<String, Integer> termEntry : topTermsWithFrequency) {
                System.out.println("   - " + termEntry.getKey() + " (Frequency: " + termEntry.getValue() + ")");
            }

            // Print Term Positions
            System.out.println("📍 **Term Positions in Document:**");
            for (Map.Entry<String, List<Integer>> positionEntry : termPositions.entrySet()) {
                System.out.println("   - " + positionEntry.getKey() + " → " + positionEntry.getValue());
            }

            // Print Child URLs
            System.out.println("🌐 **Related Child URLs:**");
            if (termUrls.containsKey(docId)) {
                for (String childUrl : termUrls.get(docId)) {
                    System.out.println("   - " + childUrl);
                }
            } else {
                System.out.println("   - No child URLs found.");
            }

            System.out.println("Context:" +snapWords);

            System.out.println("\n--------------------------------------\n");
            i++;
        }
    }

    // Helper method to process words by removing stopwords and applying stemming
    public static Map<String,  WordInfo> processWords(Vector<String> words, StopStem stopStem) {
        Map<String, WordInfo> wordInfoMap = new HashMap<>();
        int position = 1;
        for (String word : words) {
            if (!stopStem.isStopWord(word)) {
                String stemmedWord = stopStem.stem(word.toLowerCase());
                WordInfo wordInfo = wordInfoMap.getOrDefault(stemmedWord, new WordInfo());
                wordInfo.addPositionAndIncrementFrequency(position);
                wordInfoMap.put(stemmedWord, wordInfo);
            }
            position++;
        }
        return wordInfoMap;
    }


    public static Vector<String> tokenizeTitle(String title) {
        if (title == null || title.isEmpty()) {
            return new Vector<>(); // Return an empty Vector
        }
        // Split the title into words and convert to Vector
        return new Vector<>(Arrays.asList(title.split("\\s+")));
    }




}
