import java.util.*;


class Lexer {
    private final SymbolTable symbolTable = new SymbolTable();

    private static final List<String> REGEX_PATTERNS = Arrays.asList(
            "^(int|float|char|bool)$",  // Datatype
            "^(true|false)$",           // Boolean literal
            "^[a-z]+$",                 // Identifier
            "^\'.\'$",                  // Character
            "^[0-9]+$",                 // Number
            "^[0-9]+(\\.[0-9]+)?$",     // Decimal
            "^[=+\\-*/%^]$",            // Operator
            "^//.*$",                   // Single-line comment
            "/\\*.*?\\*/"               // Multi-line comment
    );

    private static final List<String> TOKENS = Arrays.asList(
            "DATATYPE",        // Matches "int", "float", etc.
            "BOOLEAN_LITERAL", // Matches "true" or "false"
            "IDENTIFIER",      // Matches variable names
            "CHARACTER",       // Matches character literals like 'a'
            "NUMBER",          // Matches integer
            "DECIMAL",         // Matches floating numbers
            "OPERATOR",        // Matches +, -, *, /, etc.
            "COMMENT",         // Matches // comments
            "MULTI_COMMENT"    // Matches /* ... */ comments
    );

    private List<DFA> dfas;

    public Lexer() {
        InitializeLexer();
    }
    public void InitializeLexer(){
        List<RegexRule> regexRules = new ArrayList<>();
        for (int i = 0; i < REGEX_PATTERNS.size(); i++) {
            String pattern = REGEX_PATTERNS.get(i);
            String token = TOKENS.get(i);

            RegexRule newRule = new RegexRule(pattern,token);
            regexRules.add(newRule);
        }

        List<NFA> nfas = new ArrayList<>();
        for (RegexRule rule : regexRules) {
            NFA newnfa = Thompson.regexToNFA(rule.getRegex());
            newnfa.TOKEN_TYPE = rule.getTokenType();
            nfas.add(newnfa);
        }

        dfas = new ArrayList<>();
        for (NFA nfa : nfas) {
            DFA newdfa = new DFA(nfa);
            newdfa.minimize();
            newdfa.TOKEN_TYPE = nfa.TOKEN_TYPE;
            dfas.add(newdfa);
            System.out.println(newdfa.TOKEN_TYPE);
            newdfa.printTransitionTable();
            System.out.println();
        }
    }

    public List<Token> tokenize(String code) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < code.length()) {
            // Skip whitespace before processing tokens
            if (Character.isWhitespace(code.charAt(i))) {
                i++;
                continue;
            }

            String match = null;
            String tokenType = null;
            int maxLength = 0;

            // Check delimiters separately
            if (";,(){}".contains(String.valueOf(code.charAt(i)))) {
                String delimiter = String.valueOf(code.charAt(i));
                tokens.add(new Token("DELIMITER", String.valueOf(code.charAt(i))));
                symbolTable.insert(delimiter, "DELIMITER");
                i++;
                continue;
            }

            // Try to match with DFA for longest possible token
            for (DFA dfa : dfas) {
                MatchResult result = dfa.match(code.substring(i));

                if (result.isMatched() && result.getLength() > maxLength) {
                    maxLength = result.getLength();
                    match = code.substring(i, i + maxLength);
                    tokenType = dfa.TOKEN_TYPE;
                }
            }

            if (match != null) {
                tokens.add(new Token(tokenType, match));

                if(!tokenType.equals("MULTI_COMMENT") && !tokenType.equals("COMMENT"))
                {
                    symbolTable.insert(match,tokenType);
                }


                i += maxLength;
            } else {
                ErrorHandler errorHandler = new ErrorHandler();
                errorHandler.THROW_LEXICAL_ERROR(i,code.charAt(i));
            }
        }
        return tokens;
    }

    public void printSymbolTable() {
        symbolTable.printTable();
    }
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

}
