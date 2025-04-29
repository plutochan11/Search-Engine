package hk.ust.csit5930.models;

import java.util.List;
import java.util.Map;

/**
 * Model class representing search results to be returned by the API
 */
public class SearchResult {
    private int docId;
    private String title;
    private String url;
    private double score;
    private double pageRankScore;
    private List<String> snippets;

    public SearchResult() {
    }

    public SearchResult(int docId, String title, String url, double score, 
                       double pageRankScore, List<String> snippets) {
        this.docId = docId;
        this.title = title;
        this.url = url;
        this.score = score;
        this.pageRankScore = pageRankScore;
        this.snippets = snippets;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getPageRankScore() {
        return pageRankScore;
    }

    public void setPageRankScore(double pageRankScore) {
        this.pageRankScore = pageRankScore;
    }

    public List<String> getSnippets() {
        return snippets;
    }

    public void setSnippets(List<String> snippets) {
        this.snippets = snippets;
    }
}