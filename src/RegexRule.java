class RegexRule {
    private final String regex;
    private final String tokenType;

    public RegexRule(String regex, String tokenType) {
        this.regex = regex;
        this.tokenType = tokenType;
    }

    public String getRegex() {
        return regex;
    }

    public String getTokenType() {
        return tokenType;
    }
}