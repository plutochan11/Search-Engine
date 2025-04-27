
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
            System.out.println("Crawler Duration: " + (crawlerEndTime - start_time));

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
            System.out.println("Indexing Duration: " + (indexEndTime - crawlerEndTime));

            //testing
            // Run PageRank using the matrix
            PageRank pageRank = new PageRank(linkMatrix);
            pageRank.computePageRank(5, 0.8);
            Map<Integer, Double> pageRankScores = pageRank.getPageRankScores();

            Vector<String> query = new Vector<>(Arrays.asList("hong", "is", "Kong"));
            List<String> filterQuery = new ArrayList<>(processWords(query, stopStem).keySet()); // Correct conversion
            Map<Integer, Double> cosineScores =CosSim.calculateCosSim(filterQuery, termToTermId, bodyInvertedIndex.getHashtable(), indexedDocs.size());

            // Combine PageRank with cosine similarity
            Map<Integer, Double> combinedScores = new HashMap<>();

            for (Integer docId : cosineScores.keySet()) {
                double cosSimScore = cosineScores.get(docId);
                double pageRankScore = pageRankScores.getOrDefault(docId, 0.0); // Default PageRank to 0 if missing
                combinedScores.put(docId, cosSimScore * pageRankScore); // Adjust ranking
            }

            // Rank documents based on combined scores
            List<Map.Entry<Integer, Double>> rankedDocs = new ArrayList<>(combinedScores.entrySet());
            rankedDocs.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); // Sort descending

            // Print the final ranked documents
            System.out.println("\nFinal Ranked Documents (CosSim * PageRank):");
            for (Map.Entry<Integer, Double> entry : rankedDocs) {
                System.out.printf("Document %d -> Score: %.5f%n", entry.getKey(), entry.getValue());
            }



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


}
