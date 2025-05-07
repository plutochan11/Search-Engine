package com.hkust.searchengine.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TermData {
    private String term;
    private int frequency;
    private List<Integer> titlePositions;
    private List<Integer> bodyPositions;

    public TermData(String term, int frequency) {
        this.term = term;
        this.frequency = frequency;
        this.titlePositions = new ArrayList<>();
        this.bodyPositions = new ArrayList<>();
    }

    // Default constructor for Jackson
    public TermData() {
        this.titlePositions = new ArrayList<>();
        this.bodyPositions = new ArrayList<>();
    }
}