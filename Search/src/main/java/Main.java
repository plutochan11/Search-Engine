
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
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
            //InvertedIndex titleInvertedIndex = new InvertedIndex("recordmanager1", "titleIndex");
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

            // Initialize SearchEngine
            SearchEngine searchEngine = new SearchEngine(stopStem, termToTermId, bodyInvertedIndex.getHashtable(), pageRankScores, indexedDocs.size());
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

                // Check if search results are empty
                if (results.isEmpty()) {
                    System.out.println("No relevant documents found for your search query.");
                } else {
                    // Print cosine similarity results (with positions)
                    System.out.println("\nTop 10 Results (Based on Cosine Similarity):");
                    List<Map.Entry<Integer, Object[]>> cosineResults = (List<Map.Entry<Integer, Object[]>>) results.get("cosine");

                    for (Map.Entry<Integer, Object[]> entry : cosineResults) {
                        int docId = entry.getKey();
                        Object[] data = entry.getValue();
                        double cosSimScore = (double) data[0];
                        List<Integer> positions = (List<Integer>) data[1]; // Term positions
                        String url = crawler.getUrlFromDocId(docId)[0];
                        String title = crawler.getUrlFromDocId(docId)[1];
                        Vector<String> content = crawler.getWordFromDocId(docId); // Retrieve full document content

                        System.out.printf("DocID: %d | URL: %s | CosSim: %.5f%n", docId, url, cosSimScore);
                        System.out.println("Title: " + title);

                        // Accumulate context snippets
                        StringBuilder fullSnippet = new StringBuilder();
                        for (int pos : positions) {
                            String contextSnippet = getSurroundingWords(content, pos);
                            fullSnippet.append(contextSnippet).append(" ... "); // Append snippet with separator
                        }

                        // Print the complete context once
                        System.out.println("Context: " + fullSnippet.toString().trim());

                    }

                    // Print combined score results (with positions)
                    System.out.println("\nTop 10 Results (Based on Combined Score - CosSim * PageRank):");
                    List<Map.Entry<Integer, Object[]>> combinedResults = (List<Map.Entry<Integer, Object[]>>) results.get("combined");

                    for (Map.Entry<Integer, Object[]> entry : combinedResults) {
                        int docId = entry.getKey();
                        Object[] data = entry.getValue();
                        double cosSimScore = (double) data[0];
                        double pageRankScore = (double) data[1];
                        double combinedScore = (double) data[2];
                        List<Integer> positions = (List<Integer>) data[3]; // Term positions
                        String url = crawler.getUrlFromDocId(docId)[0];
                        String title = crawler.getUrlFromDocId(docId)[1];
                        Vector<String> content = crawler.getWordFromDocId(docId); // Retrieve full document content

                        System.out.printf("DocID: %d | URL: %s | CosSim: %.5f | PageRank: %.5f | Combined Score: %.5f%n",
                                docId, url, cosSimScore, pageRankScore, combinedScore);
                        System.out.println("Title: " + title);
                        // Accumulate context snippets
                        StringBuilder fullSnippet = new StringBuilder();
                        for (int pos : positions) {
                            String contextSnippet = getSurroundingWords(content, pos);
                            fullSnippet.append(contextSnippet).append(" ... "); // Append snippet with separator
                        }

                        // Print the complete context once
                        System.out.println("Context: " + fullSnippet.toString().trim());
                    }
                    long searchEndTime = System.currentTimeMillis();
                    System.out.println("Searching Time: " + (searchEndTime - searchStartTime)/1000.0);
                }
            }
            scanner.close(); // Close scanner when finished

        } catch (IOException | ParserException e) {
        System.err.println("Error: " + e.getMessage());
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

    public static String getSurroundingWords(Vector<String> content, int position) {
        List<String> wordList = new ArrayList<>(content); // Convert Vector to List<String>

        if (position < 0 || position >= wordList.size()) {
            return "Context unavailable"; // Edge case handling
        }
        int start = Math.max(0, position - 2); // 2 words before
        int end = Math.min(wordList.size(), position + 3); // 2 words after (inclusive of query term)

        return String.join(" ", wordList.subList(start, end)); // Extract surrounding words
    }

}
