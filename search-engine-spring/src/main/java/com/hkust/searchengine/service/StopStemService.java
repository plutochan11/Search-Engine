package com.hkust.searchengine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

// import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Service
public class StopStemService {
    private Set<String> stopWords;
    private Porter porter;
    
    @Value("${search.engine.stopwords-file:stopwords.txt}")
    private String stopwordsFile;

    public StopStemService() {
        stopWords = new HashSet<>();
        porter = new Porter();
    }
    
    @PostConstruct
    public void init() {
        try {
            // Try to load from classpath resource first
            try (InputStream is = new ClassPathResource(stopwordsFile).getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                loadStopWordsFromReader(br);
            } catch (Exception e) {
                // Fall back to file system
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(stopwordsFile)))) {
                    loadStopWordsFromReader(br);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading stop words file: " + e.getMessage(), e);
        }
    }
    
    private void loadStopWordsFromReader(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            stopWords.add(line.trim().toLowerCase());
        }
    }

    public boolean isStopWord(String word) {
        return stopWords.contains(word.toLowerCase());
    }

    public String stem(String word) {
        return porter.stripAffixes(word);
    }
    
    /**
     * Porter stemmer implementation
     * Based on the Porter stemming algorithm by M.F. Porter
     */
    public class Porter {
        private String B;  // buffer for word to be stemmed
        private int I_end; // offset to the end of the string
        private int I_j;  // internal 'j' offset
        private int I_k;  // internal 'k' offset

        /**
         * Strip affixes from a word
         * @param word Word to stem
         * @return Stemmed word
         */
        public String stripAffixes(String word) {
            if (word == null || word.length() < 3) {
                return word;
            }
            
            B = word.toLowerCase();
            I_end = word.length() - 1;
            
            // Step 1a
            if (B.endsWith("sses")) {
                I_end -= 2;
            } else if (B.endsWith("ies")) {
                I_end -= 2;
            } else if (B.endsWith("ss")) {
                // Do nothing
            } else if (B.endsWith("s") && !isConsonantAt(I_end - 1)) {
                I_end -= 1;
            }
            
            // Step 1b
            boolean step1bDone = false;
            if (B.endsWith("eed")) {
                if (measure(0, I_end - 3) > 0) {
                    I_end -= 1;
                }
            } else if ((B.endsWith("ed") && containsVowel(0, I_end - 2)) ||
                      (B.endsWith("ing") && containsVowel(0, I_end - 3))) {
                I_end = B.endsWith("ed") ? I_end - 2 : I_end - 3;
                step1bDone = true;
                
                if (B.charAt(I_end) == 'a' && B.charAt(I_end - 1) == 't') {
                    setTo("ate");
                } else if (B.charAt(I_end) == 'b' && B.charAt(I_end - 1) == 'l') {
                    setTo("ble");
                } else if (B.charAt(I_end) == 'i' && B.charAt(I_end - 1) == 'z') {
                    setTo("ize");
                } else if (doubleCons(I_end) && 
                          !(B.charAt(I_end) == 'l' || B.charAt(I_end) == 's' || B.charAt(I_end) == 'z')) {
                    I_end -= 1;
                } else if (measure(0, I_end) == 1 && cvc(I_end)) {
                    setTo("e");
                }
            }
            
            // Step 1c
            if (B.charAt(I_end) == 'y' && containsVowel(0, I_end - 1)) {
                B = B.substring(0, I_end) + 'i';
            }
            
            // Step 2
            switch (B.charAt(I_end - 1)) {
                case 'a':
                    if (ends("ational")) { setTo("ate"); break; }
                    if (ends("tional")) { setTo("tion"); break; }
                    break;
                case 'c':
                    if (ends("enci")) { setTo("ence"); break; }
                    if (ends("anci")) { setTo("ance"); break; }
                    break;
                case 'e':
                    if (ends("izer")) { setTo("ize"); break; }
                    break;
                case 'l':
                    if (ends("bli")) { setTo("ble"); break; }
                    if (ends("alli")) { setTo("al"); break; }
                    if (ends("entli")) { setTo("ent"); break; }
                    if (ends("eli")) { setTo("e"); break; }
                    if (ends("ousli")) { setTo("ous"); break; }
                    break;
                case 'o':
                    if (ends("ization")) { setTo("ize"); break; }
                    if (ends("ation")) { setTo("ate"); break; }
                    if (ends("ator")) { setTo("ate"); break; }
                    break;
                case 's':
                    if (ends("alism")) { setTo("al"); break; }
                    if (ends("iveness")) { setTo("ive"); break; }
                    if (ends("fulness")) { setTo("ful"); break; }
                    if (ends("ousness")) { setTo("ous"); break; }
                    break;
                case 't':
                    if (ends("aliti")) { setTo("al"); break; }
                    if (ends("iviti")) { setTo("ive"); break; }
                    if (ends("biliti")) { setTo("ble"); break; }
                    break;
                case 'g':
                    if (ends("logi")) { setTo("log"); break; }
                    break;
            }
            
            // Step 3
            switch (B.charAt(I_end)) {
                case 'e':
                    if (ends("icate")) { setTo("ic"); break; }
                    if (ends("ative")) { I_end = I_j; break; }
                    if (ends("alize")) { setTo("al"); break; }
                    break;
                case 'i':
                    if (ends("iciti")) { setTo("ic"); break; }
                    break;
                case 'l':
                    if (ends("ical")) { setTo("ic"); break; }
                    if (ends("ful")) { I_end = I_j; break; }
                    break;
                case 's':
                    if (ends("ness")) { I_end = I_j; break; }
                    break;
            }
            
            // Step 4
            switch (B.charAt(I_end - 1)) {
                case 'a':
                    if (ends("al")) break; return B.substring(0, I_end + 1);
                case 'c':
                    if (ends("ance")) break;
                    if (ends("ence")) break; return B.substring(0, I_end + 1);
                case 'e':
                    if (ends("er")) break; return B.substring(0, I_end + 1);
                case 'i':
                    if (ends("ic")) break; return B.substring(0, I_end + 1);
                case 'l':
                    if (ends("able")) break;
                    if (ends("ible")) break; return B.substring(0, I_end + 1);
                case 'n':
                    if (ends("ant")) break;
                    if (ends("ement")) break;
                    if (ends("ment")) break;
                    if (ends("ent")) break; return B.substring(0, I_end + 1);
                case 'o':
                    if (ends("ion") && (B.charAt(I_j) == 's' || B.charAt(I_j) == 't')) break;
                    if (ends("ou")) break; return B.substring(0, I_end + 1);
                case 's':
                    if (ends("ism")) break; return B.substring(0, I_end + 1);
                case 't':
                    if (ends("ate")) break;
                    if (ends("iti")) break; return B.substring(0, I_end + 1);
                case 'u':
                    if (ends("ous")) break; return B.substring(0, I_end + 1);
                case 'v':
                    if (ends("ive")) break; return B.substring(0, I_end + 1);
                case 'z':
                    if (ends("ize")) break; return B.substring(0, I_end + 1);
                default:
                    return B.substring(0, I_end + 1);
            }
            
            if (measure(0, I_j) > 1) {
                I_end = I_j;
            }
            
            // Step 5a
            if (B.charAt(I_end) == 'e') {
                if (measure(0, I_end) > 1 || (measure(0, I_end) == 1 && !cvc(I_end - 1)))
                    I_end--;
            }
            
            // Step 5b
            if (measure(0, I_end) > 1 && B.charAt(I_end) == 'l' && B.charAt(I_end - 1) == 'l') {
                I_end--;
            }
            
            return B.substring(0, I_end + 1);
        }
        
        private boolean isConsonantAt(int i) {
            if (i < 0 || i > I_end) return false;
            
            switch (B.charAt(i)) {
                case 'a': case 'e': case 'i': case 'o': case 'u':
                    return false;
                case 'y':
                    return (i == 0) ? true : !isConsonantAt(i - 1);
                default:
                    return true;
            }
        }
        
        private boolean containsVowel(int start, int end) {
            for (int i = start; i <= end; i++) {
                if (!isConsonantAt(i)) return true;
            }
            return false;
        }
        
        private int measure(int start, int end) {
            int count = 0;
            boolean inConsGroup = false;
            
            for (int i = start; i <= end; i++) {
                boolean isCons = isConsonantAt(i);
                if (isCons && !inConsGroup) {
                    inConsGroup = true;
                } else if (!isCons && inConsGroup) {
                    count++;
                    inConsGroup = false;
                }
            }
            
            return count;
        }
        
        private boolean doubleCons(int j) {
            if (j < 1) return false;
            if (B.charAt(j) != B.charAt(j - 1)) return false;
            return isConsonantAt(j);
        }
        
        private boolean cvc(int i) {
            if (i < 2 || !isConsonantAt(i) || isConsonantAt(i - 1) || !isConsonantAt(i - 2)) return false;
            int ch = B.charAt(i);
            return !(ch == 'w' || ch == 'x' || ch == 'y');
        }
        
        private boolean ends(String s) {
            int len = s.length();
            if (s.charAt(len - 1) != B.charAt(I_end)) return false;
            
            I_j = I_end - len + 1;
            if (I_j < 0) return false;
            
            for (int i = 0; i < len; i++) {
                if (B.charAt(I_j + i) != s.charAt(i)) return false;
            }
            
            return true;
        }
        
        private void setTo(String s) {
            int len = s.length();
            int offset = I_j + 1;
            
            B = B.substring(0, offset) + s + B.substring(I_end + 1);
            I_end = offset + len - 1;
        }
    }
}