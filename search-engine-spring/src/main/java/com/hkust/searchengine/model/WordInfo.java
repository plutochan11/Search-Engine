package com.hkust.searchengine.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WordInfo {
    private int frequency;
    private List<Integer> positions;

    public WordInfo() {
        this.frequency = 0;
        this.positions = new ArrayList<>();
    }

    public void addPositionAndIncrementFrequency(int position) {
        positions.add(position);
        frequency++;
    }
}