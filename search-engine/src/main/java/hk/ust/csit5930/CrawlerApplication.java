package hk.ust.csit5930;

public class CrawlerApplication {
    public static void main(String[] args) {
        Spider spider = new Spider();
        spider.crawl();
        spider.checkResults();
    }
}