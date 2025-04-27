import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class PageRank {
    private int[][] linkMatrix;
    private int numPages;
    private double[] ranks;

    public PageRank(int[][] linkMatrix) {
        this.linkMatrix = linkMatrix;
        this.numPages = linkMatrix.length;
        this.ranks = new double[numPages];
        Arrays.fill(ranks, 1.0 / numPages); // Initialize rank vector
    }

    public void computePageRank(int iterations, double dampingFactor) {
        double[] prevRanks = new double[numPages];

        for (int iter = 0; iter < iterations; iter++) {
            System.arraycopy(ranks, 0, prevRanks, 0, numPages); // Copy previous ranks

            for (int i = 0; i < numPages; i++) {
                double rankSum = 0.0;
                for (int j = 0; j < numPages; j++) {
                    if (linkMatrix[j][i] == 1) {
                        int outgoingLinks = Arrays.stream(linkMatrix[j]).sum();
                        rankSum += prevRanks[j] / outgoingLinks;
                    }
                }
                ranks[i] = (1 - dampingFactor) / numPages + dampingFactor * rankSum;
            }
        }

        normalizeRanks();
    }

    private void normalizeRanks() {
        double sum = Arrays.stream(ranks).sum();
        for (int i = 0; i < numPages; i++) {
            ranks[i] /= sum;
        }
    }
    public Map<Integer, Double> getPageRankScores() {
        Map<Integer, Double> pageRankScores = new HashMap<>();
        for (int i = 0; i < numPages; i++) {
            pageRankScores.put(i + 1, ranks[i]); // Mapping docId (1-based index) to PageRank
        }
        return pageRankScores;
    }


    public void printRanks() {
        System.out.println("PageRank Scores:");
        for (int i = 0; i < numPages; i++) {
            System.out.printf("Page %d: %.5f%n", i + 1, ranks[i]);
        }
    }
}
