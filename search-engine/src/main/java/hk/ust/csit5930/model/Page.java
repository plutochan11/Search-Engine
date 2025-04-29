package hk.ust.csit5930.model;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Page POJO that represents a web page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page {
    private int id;
    private String url;
    private String title;
    private String content;
    private Timestamp lastModified;
    private int size;

    public Page(String url, String title, String content, Timestamp lastCrawled, int size) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.lastModified = lastCrawled;
        this.size = size;
    }
}