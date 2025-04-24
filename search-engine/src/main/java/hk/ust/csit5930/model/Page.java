package hk.ust.csit5930.model;

import java.sql.Timestamp;

public class Page {
    private int id;
    private String url;
    private String title;
    private String content;
    private Timestamp lastModified;
    
    public Page() {
    }

    public Page(String url, String title, String content, Timestamp lastCrawled) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.lastModified = lastCrawled;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    public void setLastModified(Timestamp lastCrawled) {
        this.lastModified = lastCrawled;
    }
}