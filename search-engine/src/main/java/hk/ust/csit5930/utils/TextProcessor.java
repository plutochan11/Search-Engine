package hk.ust.csit5930.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import hk.ust.csit5930.models.WordInfo;

/**
 * Utility class for processing text data in the search engine
 */
public class TextProcessor {
    
    /**
     * Process words by removing stopwords and applying stemming
     * @param words Vector of words to process
     * @param stopStem StopStem instance for stopword removal and stemming
     * @return Map of stemmed words to their word info (frequency and positions)
     */
    public static Map<String, WordInfo> processWords(Vector<String> words, StopStem stopStem) {
        Map<String, WordInfo> wordInfoMap = new HashMap<>();
        int position = 1;
        for (String word : words) {
            // Skip empty strings to prevent StringIndexOutOfBoundsException
            if (word == null || word.trim().isEmpty()) {
                continue;
            }
            
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

    /**
     * Get surrounding words for context in search results
     * @param content Vector of all words in the document
     * @param position Position of the query term in the document
     * @return String containing the query term and surrounding context words
     */
    public static String getSurroundingWords(Vector<String> content, int position) {
        if (content == null || content.isEmpty()) {
            return "Context unavailable";
        }
        
        if (position < 0 || position >= content.size()) {
            return "Context unavailable"; // Edge case handling
        }
        
        int start = Math.max(0, position - 2); // 2 words before
        int end = Math.min(content.size(), position + 3); // 2 words after (inclusive of query term)

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                snippet.append(" ");
            }
            snippet.append(content.get(i));
        }
        
        return snippet.toString();
    }
}