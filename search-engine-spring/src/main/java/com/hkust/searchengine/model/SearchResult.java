package com.hkust.searchengine.model;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class SearchResult {
    private int docId;
    private String title;
    private String url;
    private double score;
    private double pageRankScore;
    private double combinedScore;
    private Date lastModified;
    private long pageSize;
    private List<Map.Entry<String, Integer>> topTerms; // Keywords with frequencies
    private Map<String, List<Integer>> titleTermPositions;
    private Map<String, List<Integer>> bodyTermPositions;
    private List<String> parentUrls; // Parent links
    private List<String> childUrls;  // Child links
    private List<String> snippets;
    
    // Default constructor for Jackson
    public SearchResult() {
    }
    
    public SearchResult(int docId, String title, String url, double score,
                       Date lastModified, long pageSize,
                       List<Map.Entry<String, Integer>> topTerms,
                       Map<String, List<Integer>> titleTermPositions, 
                       Map<String, List<Integer>> bodyTermPositions,
                       List<String> parentUrls,
                       List<String> childUrls,
                       List<String> snippets) {
        this.docId = docId;
        this.title = title;
        this.url = url;
        this.score = score;
        this.lastModified = lastModified;
        this.pageSize = pageSize;
        this.topTerms = topTerms;
        this.titleTermPositions = titleTermPositions;
        this.bodyTermPositions = bodyTermPositions;
        this.parentUrls = parentUrls;
        this.childUrls = childUrls;
        this.snippets = snippets;
        // Initialize with defaults
        this.pageRankScore = 0.0;
        this.combinedScore = score; // Default to same as score
    }
}