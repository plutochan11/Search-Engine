package hk.ust.csit5930;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jsoup.Jsoup;
import org.apache.ibatis.exceptions.PersistenceException;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import hk.ust.csit5930.model.Page;
import hk.ust.csit5930.model.Relationship;

public class Spider {
    // Default entry point, can delegate users to pass their own
    private String URL;
    private final int NUM_PAGES = 30; // Number of pages to crawl
    private static H2DBOperator dbOperator;

    public Spider () {
        URL = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
        dbOperator = new H2DBOperator();
        dbOperator.setup(); // Setup the database
    }

    // User-specified-entry version of constructor
    public Spider (String entryUrl) {
        URL = entryUrl;
        dbOperator = new H2DBOperator();
        // dbOperator.setup(); // Setup the database
    }

    public void crawl() {
        // Count to keep track of the number of pages crawled successfully
        int pageCount = 0;

        // Implement BSF with a queue (FIFO)
        Queue<String> urlQueue = new LinkedList<>();
        urlQueue.add(URL); // Start with the initial URL

        // Check duplicates with a hashset (O(1) lookup)
        Set<String> visitedUrls = new HashSet<>();

        while (pageCount < NUM_PAGES) {
            String url;
            try {
                // Dequeue the url and fetch the page
                // Check for availability
                if ((url = urlQueue.poll()) == null) {
                    System.out.println("No more URLs to crawl.");
                    break;
                }

                Response urlResponse = Jsoup.connect(url).execute();

                // Extract the last modified date from headers and parse it to a Timestamp
                Map<String, String> headers = urlResponse.headers();
                Timestamp lastModified = parseLastModified(headers.get("Last-Modified")); 

                // Parse the DOM to fetch title and content
                Document urlDoc = urlResponse.parse();
                String urlTitle = urlDoc.title(); 
                String urlContent = urlDoc.body().text();

                // Insert the page into pages table
                try {
                    dbOperator.insert(url, urlTitle, urlContent, lastModified);
                } catch (PersistenceException e) { // Catch the exception manually to handle update scenarios
                    // Check the update results. If it's 0, it means there's no update and thereby next page
                    if ((dbOperator.updateById(url, urlTitle, urlContent, lastModified)) == 0) {
                        continue;
                    }
                }

                visitedUrls.add(url);
                pageCount++;
                System.out.println("Fetched " + pageCount + " pages");

                /*
                    Find links in the page and queue them
                */ 
                urlDoc.select("a[href]").forEach(anchor -> {
                    // Retain the absolute urls
                    String link = anchor.attr("abs:href");
                    if (!urlQueue.contains(link) && !visitedUrls.contains(link)) {
                        
                        // Record parent-child relationship
                        // dbOperator.insertPlaceHolder (link);
                        dbOperator.insertRelationship (url, link);
                        urlQueue.add(link);
                    }
                });

            } catch (IOException e) {
                System.err.println("Failed to crawl URL");
                e.printStackTrace();
            }
        }

        System.out.println("Finished crawling " + pageCount + " pages.");
    }

    private Timestamp parseLastModified(String lastModifiedDate) {
        Timestamp lastModified = null; // It's possible that the page doesn't have a last modified date
        if (lastModifiedDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            ZonedDateTime zdt = ZonedDateTime.parse(lastModifiedDate, formatter);
            Instant instant = zdt.toInstant();
            lastModified = Timestamp.from(instant);
        }
        return lastModified;
    }

    public void checkResults() {
        List<Page> pages = dbOperator.getAllPages();
        pages.forEach(page -> {
            System.out.printf("Page ID: %s, Title: %s\n", 
                        page.getId(), page.getTitle());
        });
        List<Relationship> relationships = dbOperator.getAllRelationships();
        relationships.forEach(relationship -> {
            System.out.printf("Relationship ID: %s, Parent URL: %s, Child URL: %s\n",
                        relationship.getId(), relationship.getParentUrl(), relationship.getChildUrl());
        });
    }
}
