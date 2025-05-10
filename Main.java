
import java.io.IOException;
import org.htmlparser.util.ParserException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String rootUrl = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"; // URL to crawl and index
        try {
            // Initialize the crawler
            Crawler crawler = new Crawler(rootUrl);
            StopStem stopStem = new StopStem("stopwords.txt");
            InvertedIndex titleInvertedIndex = new InvertedIndex("recordmanager1", "titleIndex");
            InvertedIndex bodyInvertedIndex = new InvertedIndex("recordmanager2", "bodyIndex");

            // Maintain term index mapping
            Map<String, Integer> termToTermId = new HashMap<>();
            Map<Integer, String> termIdToTerm = new HashMap<>();
            int nextTermId = 1;

            // Step 1: Crawl all links and assign document IDs
            Map<Integer, List<Integer>> indexedDocs = crawler.crawlAllLinks();

            // Step 2: Process each page title & body separately
            Map<Integer, Map<Integer, Integer>> docTermIndex = new HashMap<>(); // Stores (docId → termId → frequency)

            for (Integer docId : indexedDocs.keySet()) {
                String url = crawler.getUrlFromDocId(docId);
                Crawler pageCrawler = new Crawler(url);

                // Extract title & body words separately
                Vector<String> titleWords = pageCrawler.extractTitleWords();
                Vector<String> bodyWords = pageCrawler.extractWords();

                // Process words separately
                Map<String, Integer> titleWordFreq = processWords(titleWords, stopStem);
                Map<String, Integer> bodyWordFreq = processWords(bodyWords, stopStem);

                // Process and assign term IDs
                Map<Integer, Integer> termFreqMap = new HashMap<>();

                // Assign term IDs for title words
                for (Map.Entry<String, Integer> entry : titleWordFreq.entrySet()) {
                    String term = entry.getKey();
                    int frequency = entry.getValue();

                    if (!termToTermId.containsKey(term)) {
                        termToTermId.put(term, nextTermId);
                        termIdToTerm.put(nextTermId, term);
                        nextTermId++;
                    }

                    int termId = termToTermId.get(term);
                    termFreqMap.put(termId, frequency);
                    titleInvertedIndex.addEntry(term, docId, frequency); // Index for title
                }

                // Assign term IDs for body words
                for (Map.Entry<String, Integer> entry : bodyWordFreq.entrySet()) {
                    String term = entry.getKey();
                    int frequency = entry.getValue();

                    if (!termToTermId.containsKey(term)) {
                        termToTermId.put(term, nextTermId);
                        termIdToTerm.put(nextTermId, term);
                        nextTermId++;
                    }

                    int termId = termToTermId.get(term);
                    termFreqMap.put(termId, frequency);
                    bodyInvertedIndex.addEntry(term, docId, frequency); // Index for body
                }
                // Store document-term index
                //should we separate the frequency for title and body?
                docTermIndex.put(docId, termFreqMap);
            }
            // Print document-term index (docId, termId, frequency)
            System.out.println("Document-Term Index:");
            for (Map.Entry<Integer, Map<Integer, Integer>> docEntry : docTermIndex.entrySet()) {
                int docId = docEntry.getKey();
                for (Map.Entry<Integer, Integer> termEntry : docEntry.getValue().entrySet()) {
                    int termId = termEntry.getKey();
                    int frequency = termEntry.getValue();
                    System.out.println("DocID: " + docId + ", TermID: " + termId + ", Frequency: " + frequency);
                }
            }
            // Step 3: Print indexed information
            System.out.println("Title Index:");
            titleInvertedIndex.printAll();

            System.out.println("Body Index:");
            bodyInvertedIndex.printAll();

            // Print document ID mappings
            crawler.printDocMapping();

            // Print document linkage (Doc ID → List of linked Doc IDs)
            for (Map.Entry<Integer, List<Integer>> entry : indexedDocs.entrySet()) {
                System.out.println("Doc " + entry.getKey() + " links to -> " + entry.getValue());
            }

        } catch (ParserException | IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // Helper method to process words by removing stopwords and applying stemming
    public static Map<String, Integer> processWords(Vector<String> words, StopStem stopStem) {
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String word : words) {
            if (!stopStem.isStopWord(word)) {
                String stemmedWord = stopStem.stem(word);
                wordFrequency.put(stemmedWord, wordFrequency.getOrDefault(stemmedWord, 0) + 1);
            }
        }
        return wordFrequency;
    }
}
