package com.hkust.searchengine.model;

import lombok.Data;

@Data
public class TermInfo {
    private String term;
    private int termId;
    private int frequency;

    public TermInfo(String term, int termId, int frequency) {
        this.term = term;
        this.termId = termId;
        this.frequency = frequency;
    }

    // Default constructor for Jackson
    public TermInfo() {
    }
}