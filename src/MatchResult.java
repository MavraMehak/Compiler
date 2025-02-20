class MatchResult {
    private final boolean matched;
    private final int length;

    public MatchResult(boolean matched, int length) {
        this.matched = matched;
        this.length = length;
    }

    public boolean isMatched() {
        return matched;
    }

    public int getLength() {
        return length;
    }
}