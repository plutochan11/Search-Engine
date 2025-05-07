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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
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
 * algorithm to crawl the web pages with multi-threading support for improved performance.
 * <p> The class also includes a retry mechanism for failed URLs, and a method
 * to handle the crawling process efficiently.
 * 
 * @author pluto
 */
public class Spider {
    // Default entry point, can delegate users to pass their own
    private String URL;
    private int NUM_PAGES = 300; // Number of pages to crawl
    private static H2DBOperator dbOperator;
    private int[][] linkMatrix; // Adjacency matrix for the graph representation of the web pages
    private int MAX_RETRIES = 2; // Maximum number of retries for failed URLs
    private int CONNECTION_TIMEOUT = 10000; // Connection timeout in milliseconds

    /**
     * Default constructor with a preset entry point.
     * The default entry point is "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm".
     * <p> This constructor initializes the database operator and sets up the database.
     */
    public Spider () {
        URL = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
        dbOperator = new H2DBOperator();
    }

    /**
     * Constructor for user-specified entry point
     * @param entryUrl The entry URL you want to start crawling with
     */
    public Spider (String entryUrl) {
        URL = entryUrl;
        dbOperator = new H2DBOperator();
    }

    /**
     * Calculate optimal thread count based on system capabilities
     * @return The optimal number of threads to use
     */
    private int calculateOptimalThreadCount() {
        // Get available processors (cores)
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // For M2 MacBook:
        // M2 chip has up to 8 cores (4 performance + 4 efficiency)
        // Use slightly more threads than cores to account for I/O waits
        // but not too many to avoid thread switching overhead
        int optimal = Math.min(availableProcessors * 2, 24);
        
        // Ensure at least 4 threads and not more than 32
        return Math.max(4, Math.min(optimal, 32));
    }

    /**
     * Crawl web pages as per the number of pages specified with multi-threading support.
     * <p> Uses a thread pool to parallelize crawling for significantly improved performance.
     * Includes retry mechanism for failed URLs.
     */
    public void crawl() {
        long startTime = System.currentTimeMillis();
        
        // Use AtomicInteger for thread-safe counter
        AtomicInteger pageCount = new AtomicInteger(0);

        // Use concurrent collections for thread safety
        ConcurrentLinkedQueue<String> urlQueue = new ConcurrentLinkedQueue<>();
        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        Set<String> inProgressUrls = ConcurrentHashMap.newKeySet();
        
        // Map to track retry counts for failed URLs
        Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();

        // Add the starting URL
        urlQueue.add(URL);
        
        // Create a thread pool with optimized size for M2 MacBook
        int numThreads = calculateOptimalThreadCount();
        System.out.println("Starting crawl with " + numThreads + " threads (optimized for your system)");
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        
        // Lock for synchronizing pageCount access
        ReentrantLock countLock = new ReentrantLock();
        
        // Submit tasks for crawling until we reach NUM_PAGES
        while (pageCount.get() < NUM_PAGES && (!urlQueue.isEmpty() || !inProgressUrls.isEmpty())) {
            
            // If queue is empty but we have URLs in progress, wait a bit
            if (urlQueue.isEmpty() && !inProgressUrls.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            
            // Get the next URL from the queue
            String url = urlQueue.poll();
            if (url == null) continue;
            
            // Mark this URL as in progress
            inProgressUrls.add(url);
            
            // Submit task to thread pool
            executorService.submit(() -> {
                try {
                    // Check if we've already reached our target page count
                    if (pageCount.get() >= NUM_PAGES) {
                        inProgressUrls.remove(url);
                        return;
                    }
                    
                    // Only process if we haven't visited this URL yet
                    if (!visitedUrls.contains(url)) {
                        try {
                            // Connect with a timeout and user agent
                            Response urlResponse = Jsoup.connect(url)
                                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)")
                                    .followRedirects(true)
                                    .timeout(CONNECTION_TIMEOUT)
                                    .execute();

                            // Extract and process headers
                            Map<String, String> headers = urlResponse.headers();
                            Timestamp lastModified = parseLastModified(headers.get("Last-Modified")); 
                            
                            // Handle content length
                            String contentLength = headers.get("Content-Length");
                            int size = 0;
                            if (contentLength != null && !contentLength.equals("0")) {
                                try {
                                    size = Integer.parseInt(contentLength);
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid Content-Length format: " + contentLength);
                                }
                            }

                            // Parse the content
                            Document urlDoc = urlResponse.parse();
                            String urlTitle = urlDoc.title(); 
                            String urlContent = urlDoc.body().text();

                            // If content length wasn't available, use the content's length
                            if (size == 0) {
                                size = urlContent.length();
                            }
                            
                            // Check if we need to add this page
                            boolean pageAdded = false;
                            
                            // Critical section for DB access and counter update
                            countLock.lock();
                            try {
                                // Make sure we haven't exceeded our page count while we were working
                                if (pageCount.get() < NUM_PAGES && !visitedUrls.contains(url)) {
                                    try {
                                        // Try to insert the page
                                        dbOperator.insert(url, urlTitle, urlContent, lastModified, size);
                                        visitedUrls.add(url);
                                        pageCount.incrementAndGet();
                                        pageAdded = true;
                                        
                                        // Log progress
                                        int currentCount = pageCount.get();
                                        if (currentCount % 30 == 0 || currentCount == 1) {
                                            System.out.println("Crawled " + currentCount + " pages");
                                        }
                                    } catch (PersistenceException e) {
                                        // Handle update for existing URLs
                                        if (dbOperator.updateById(url, urlTitle, urlContent, lastModified, size) > 0) {
                                            visitedUrls.add(url);
                                            pageCount.incrementAndGet();
                                            pageAdded = true;
                                        }
                                    }
                                }
                            } finally {
                                countLock.unlock();
                            }
                            
                            // If we successfully added this page, process its links
                            if (pageAdded && pageCount.get() < NUM_PAGES) {
                                // Extract links
                                urlDoc.select("a[href]").forEach(anchor -> {
                                    String link = anchor.attr("abs:href");
                                    
                                    // Filter valid links and avoid already processed ones
                                    if (isValidUrl(link) && !visitedUrls.contains(link) && !inProgressUrls.contains(link)) {
                                        // Record parent-child relationship
                                        dbOperator.insertRelationship(url, link);
                                        
                                        // Add to queue if we still need more pages
                                        if (pageCount.get() < NUM_PAGES) {
                                            urlQueue.add(link);
                                            inProgressUrls.add(link);
                                        }
                                    }
                                });
                            }
                        } catch (IOException e) {
                            // Handle failed URLs with the retry mechanism
                            int retryCount = retryCountMap.getOrDefault(url, 0) + 1;
                            
                            if (retryCount <= MAX_RETRIES) {
                                // Update retry count and add back to queue with lower priority (at the end)
                                retryCountMap.put(url, retryCount);
                                System.out.println("Retrying URL (attempt " + retryCount + "/" + MAX_RETRIES + "): " + url);
                                urlQueue.add(url);
                            } else {
                                // Log permanently failed URLs after max retries
                                System.err.println("Failed to crawl URL after " + MAX_RETRIES + " attempts: " + url + " - " + e.getMessage());
                            }
                        }
                    }
                } finally {
                    // Always remove from in-progress set when done
                    inProgressUrls.remove(url);
                }
            });
        }

        // Shutdown the executor and wait for all tasks to complete
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                System.out.println("Executor timed out, forcing shutdown");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Initialize the link matrix with the number of pages crawled
        int finalPageCount = pageCount.get();
        linkMatrix = new int[finalPageCount][finalPageCount];
        
        // Print retry statistics
        int totalFailedUrls = (int) retryCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() > MAX_RETRIES)
                .count();
                
        System.out.println("Total URLs that failed after max retries: " + totalFailedUrls);

        long endTime = System.currentTimeMillis();
        System.out.printf("Successfully crawled %d pages in %s s\n", finalPageCount, (endTime - startTime) / 1000);
    }
    
    /**
     * Check if a URL is valid and should be crawled.
     * @param url The URL to check
     * @return true if the URL is valid, false otherwise
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Skip non-HTTP/HTTPS URLs
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        
        // Skip URLs with fragments or certain file types
        if (url.contains("#") || 
            url.endsWith(".pdf") || 
            url.endsWith(".zip") || 
            url.endsWith(".jpg") || 
            url.endsWith(".jpeg") ||
            url.endsWith(".png") || 
            url.endsWith(".gif") ||
            url.endsWith(".mp4") ||
            url.endsWith(".mp3") ||
            url.endsWith(".avi") ||
            url.endsWith(".mov")) {
            return false;
        }
        
        return true;
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

    /**
     * Specify the number of webpages to crawl.
     * @param numPages The number of pages for crawling.
     */
    public void setNumPages(int numPages) {
        this.NUM_PAGES = numPages;
    }

    /**
     * Set the maximum number of retries for failed URLs
     * @param maxRetries The maximum number of retries
     */
    public void setMaxRetries(int maxRetries) {
        this.MAX_RETRIES = maxRetries;
    }
    
    /**
     * Set the connection timeout in milliseconds
     * @param timeout The timeout in milliseconds
     */
    public void setConnectionTimeout(int timeout) {
        this.CONNECTION_TIMEOUT = timeout;
    }

    /**
     * Get the ID of a page from its URL
     * @param url The URL of the page
     * @return The page ID, or -1 if not found
     */
    public int getIdFromUrl(String url) {
        return dbOperator.getIdFromUrl(url);
    }
    
    // test use
    public Page getPage(int id) {
        return dbOperator.getPage(id);
    }

    /**
     * Set up the database from scratch.
     * <p> This method initializes the database and creates the necessary tables.
     * @param flag A boolean flag to indicate whether to set up the database from scratch.
     */
    public void fromScratch(Boolean flag) {
        if (flag) {
            dbOperator.setup();
        }
    }
}
