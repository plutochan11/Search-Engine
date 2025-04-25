package hk.ust.csit5930;

public class CrawlerApplication {
    private static final String[] userChoices = {
        "1. Crawl all pages from scratch",
        "2. Generate a JSON file containing all the pages",
        "3. Generate a JSON file containing all the relationships",
        "4. Exit"
    };

    public static void main(String[] args) {
        System.out.println("Welcome to the Web Crawler. Please choose an option: ");
        for (String choice : userChoices) {
            System.out.println(choice);
        }
        System.out.println("Please enter your choice: ");
        Scanner scanner = new Scanner(System.in);
        int userChoice = scanner.nextInt();
        scanner.nextLine(); // Consume the newline character
        // Interactive menu until user chooses to exit
        while (userChoice != 4) {
            switch (userChoice) {
            case 1:
                crawlFromScratch();
                break;
            case 2:
                generateJsonFile("pages");
                break;
            case 3:
                generateJsonFile("relationships");
                break;
            case 4:
                System.out.println("Exiting the program...");
                System.exit(1);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
            }

            // Display the menu again
            System.out.println("Please choose an option: ");
            for (String choice : userChoices) {
                System.out.println(choice);
            }
            System.out.print("Please enter your choice: ");
            userChoice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character
        }
        scanner.close();

        
    }

    private static void crawlFromScratch() {
        Spider spider = new Spider();
        // long startTime = System.currentTimeMillis();
        spider.crawl();
        // long endTime = System.currentTimeMillis();
        // spider.checkResults();
        // System.out.printf("Running time: %d s\n", (endTime - startTime)/1000);
    }
}