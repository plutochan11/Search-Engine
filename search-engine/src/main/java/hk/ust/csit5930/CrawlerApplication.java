package hk.ust.csit5930;

/**
 * Crawler program entry.
 * <p> If you'd like to refactor the program to a Spring Boot application,
 * you should start with this entry and add relevant annotations.
 * 
 * @author pluto
 */
public class CrawlerApplication {
    public static void main(String[] args) {
        Spider spider = new Spider(true);
        spider.crawl();
    }
}