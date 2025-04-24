package hk.ust.csit5930;

public class CrawlerApplication {
    public static void main(String[] args) {
        Spider spider = new Spider();
        long startTime = System.currentTimeMillis();
        spider.crawl();
        long endTime = System.currentTimeMillis();
        spider.checkResults();
        System.out.printf("Running time: %d s\n", (endTime - startTime)/1000);
    }
}