package hk.ust.csit5930;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import org.jsoup.Jsoup;
import org.apache.ibatis.exceptions.PersistenceException;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import hk.ust.csit5930.model.Page;
import hk.ust.csit5930.model.Relationship;


/**
 * Spider class to crawl web pages and store them in a database.
 * <p> This class uses Jsoup to fetch and parse web pages, and H2DBOperator
 * to interact with the H2 database. It implements a breadth-first search (BFS)
 * algorithm to crawl the web pages.
 * 
 * @author pluto
 */
public class Spider {
    // Default entry point, can delegate users to pass their own
    private String URL;
    private final int NUM_PAGES = 297; // Number of pages to crawl
    private static H2DBOperator dbOperator;
    private int[][] linkMatrix; // Adjacency matrix for the graph representation of the web pages

    /**
     * Default constructor with a preset entry point.
     * The default entry point is "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm".
     * <p> This constructor initializes the database operator and sets up the database.
     * @param setup A boolean flag to indicate whether to set up the database.
     */
    public Spider (Boolean setup) {
        URL = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
        dbOperator = new H2DBOperator();
        if (setup) {
            dbOperator.setup(); // Setup the database
        }
    }

    /**
     * Constructor for user-specified entry point
     * @param entryUrl
     * @param setup A boolean flag to indicate whether to set up the database.
     */
    public Spider (String entryUrl, Boolean setup) {
        URL = entryUrl;
        dbOperator = new H2DBOperator();
        if (setup) {
            dbOperator.setup(); // Setup the database
        }
    }

    /**
     * Crawl web pages as per the number of pages specified.
     * <p> Cycles are handled by a hashset to check for duplicates.
     */
    public void crawl() {
        long startTime = System.currentTimeMillis();
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

                Response urlResponse = Jsoup.connect(url)
                        // .timeout(10000)
                        .userAgent("Mozilla/5.0")
                        .execute();

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
                // Print the intermediate results
                if (pageCount % 10 == 0 || pageCount == 1) {
                    System.out.println("Crawled " + pageCount + " pages");
                }

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

            // Show some respect to the server
            // try {
            //     Thread.sleep(100); // Sleep for 0.1 seconds to avoid overwhelming the server
            // } catch (InterruptedException e) {
            //     System.err.println("Thread interrupted");
            //     e.printStackTrace();
            // }
        }
        // Initialise the link matrix with the number of pages crawled
        linkMatrix = new int[pageCount][pageCount];

        long endTime = System.currentTimeMillis();
        System.out.printf("Successfully crawled %d pages in %s s\n", pageCount, (endTime - startTime) / 1000);
    }

    /**
     * Parse the last modified date from the HTTP headers.
     * <p> This method converts the last modified date string to a Timestamp object.
     * @param lastModifiedDate The last modified date string from the HTTP headers.
     * @return A Timestamp object representing the last modified date, or null if not available.
     */
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

    /**
     * Display all entries in the database.
     * <p> This method retrieves all pages from the database and prints them.
     */
    public void displayAllPages() {
        List<Page> pages = dbOperator.getAllPages();
        pages.parallelStream().filter(page -> page.getId() <= 10)
            .forEach(page -> {
                        System.out.printf("Page ID: %s, Title: %s, URL: %s\n", 
                                    page.getId(), page.getTitle(), page.getUrl());
            });
        // List<Relationship> relationships = dbOperator.getAllRelationships();
        // relationships.forEach(relationship -> {
        //     System.out.printf("Relationship ID: %s, Parent URL: %s, Child URL: %s\n",
        //                 relationship.getId(), relationship.getParentUrl(), relationship.getChildUrl());
        // });
    }

    /**
     * Get all relationships from the database and construct the link matrix.
     * <p> This method retrieves all relationships from the database and returns them as a map.
     * @return A map of relationships where the key is the parent page ID and the value is a list of child page IDs.
     */
    public Map<Integer, List<Integer>> getRelationships() {
        List<Relationship> relationships = dbOperator.getAllRelationships();
        Map<Integer, List<Integer>> relationshipMap = new HashMap<>();
        int parentId, childId;

        // Convert the list of relationships to a map
        for (Relationship relationship : relationships) {
            // Get the parent and child IDs
            parentId = dbOperator.getPageId(relationship.getParentUrl());
            childId = dbOperator.getPageId(relationship.getChildUrl());
            relationshipMap
                .computeIfAbsent(parentId, k -> new ArrayList<>())
                .add(childId);
            
            // Construct the link matrix as we go
            constructLinkMatrix(parentId, childId);
        }
        
        return relationshipMap;
    }

    private void constructLinkMatrix(int parentId, int childId) {
        linkMatrix[parentId - 1][childId - 1] = 1;
    }

    /**
     * Get the link matrix.
     * @return The link matrix as a 2D array.
     */
    public int[][] getLinkMatrix() {
        return linkMatrix;
    }

    /**
     * Return the page content
     * @param id The page ID
     * @return The page content as a vector of individual words.
     */
    public Vector<String> getContent(int id) {
        Vector<String> content = new Vector<>();
        String body = dbOperator.getContent(id);
        String[] words = body.split("[\\s\\n]+");
		for (String word : words) {
			content.add(word);
		}
        return content;
    }

    /**
     * Return the page URL
     * @param id The page ID
     * @return The page URL as a string.
     */
    public String getUrl(int id) {
        return dbOperator.getUrl(id);
    }

    /**
     * Return the page title
     * @param id The page ID
     * @return The page title as a string.
     */
    public String getTitle(int id) {
        return dbOperator.getTitle(id);
    }
}
