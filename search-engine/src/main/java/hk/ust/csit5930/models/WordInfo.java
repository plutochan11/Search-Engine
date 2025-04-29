package hk.ust.csit5930.models;
import java.util.ArrayList;
import java.util.List;

public class WordInfo {
    private int frequency;
    private List<Integer> positions;

    public WordInfo() {
        this.frequency = 1;
        this.positions = new ArrayList<>();
    }

    public int getFrequency() {
        return frequency;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public void addPositionAndIncrementFrequency(int position) {
        positions.add(position);
        frequency++;
    }
}