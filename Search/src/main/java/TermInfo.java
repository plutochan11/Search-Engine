public class TermInfo {
    private String term;
    public int termId;
    public int frequency;

    public TermInfo(String term, int termId, int frequency) {
        this.term = term;
        this.termId = termId;
        this.frequency = frequency;
    }

    public String getTerm() {
        return term;
    }

    public int getTermId() {
        return termId;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
