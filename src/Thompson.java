import java.util.*;

public class Thompson {
    private static int stateCounter = 0;

    // Convert Regular Expression (RE) to an NFA
    public static NFA regexToNFA(String regex) {
        Stack<NFA> nfaStack = new Stack<>();
        boolean startAnchored = regex.startsWith("^");
        boolean endAnchored = regex.endsWith("$");

        if (startAnchored) regex = regex.substring(1);  // Remove ^
        if (endAnchored) regex = regex.substring(0, regex.length() - 1);  // Remove $

        boolean shouldConcatenate = false;

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            if (c == '(') {
                int end = findMatchingParenthesis(regex, i);
                if (end == -1) throw new IllegalArgumentException("Unmatched '('");

                String group = regex.substring(i + 1, end);
                String[] options = group.split("\\|");

                NFA groupNFA = null;
                for (String option : options) {
                    NFA optionNFA = regexToNFA(option);
                    if (groupNFA == null) {
                        groupNFA = optionNFA;
                    } else {
                        groupNFA = union(groupNFA, optionNFA);
                    }
                }

                if (groupNFA != null) {
                    nfaStack.push(groupNFA);
                    shouldConcatenate = true;
                }

                i = end;
            }
            else if (c == '\'') {
                if (i + 1 < regex.length() && regex.charAt(i + 1) == '\\') {
                    // Handle escaped single quote (\')
                    if (i + 2 < regex.length() && regex.charAt(i + 2) == '\'') {
                        nfaStack.push(createSingleCharNFA('\''));
                        shouldConcatenate = true;
                        i += 2; // Skip over \'
                    } else {
                        throw new IllegalArgumentException("Invalid escaped character sequence");
                    }
                } else {
                    // Handle normal single quote
                    nfaStack.push(createSingleCharNFA('\''));
                    shouldConcatenate = true;
                }
            }
            else if (c == '/') {
                if (i + 1 < regex.length()) {
                    char next = regex.charAt(i + 1);
                    if (next == '/') {

                        NFA slashNFA = createSingleCharNFA('/');
                        NFA doubleSlashNFA = concatenate(slashNFA, createSingleCharNFA('/'));
                        nfaStack.push(doubleSlashNFA);
                        shouldConcatenate = true;
                        i++;
                    } else if (next == '*') {

                        NFA slashNFA = createSingleCharNFA('/');
                        NFA starNFA = createSingleCharNFA('*');
                        nfaStack.push(concatenate(slashNFA, starNFA));
                        shouldConcatenate = true;
                        i++;
                    } else {
                        nfaStack.push(createSingleCharNFA('/'));
                        shouldConcatenate = true;
                    }
                }
            } else if (c == '*') {
                if (i + 1 < regex.length() && regex.charAt(i + 1) == '/') {

                    NFA starNFA = createSingleCharNFA('*');
                    NFA slashNFA = createSingleCharNFA('/');
                    nfaStack.push(concatenate(starNFA, slashNFA));
                    i++;
                } else {
                    nfaStack.push(createSingleCharNFA('*'));
                }
                shouldConcatenate = true;
            } else if (c == '.') {
                if (i + 1 < regex.length() && regex.charAt(i + 1) == '*') {
                    nfaStack.push(kleeneStar(createAnyCharNFA()));
                    i++;
                } else {
                    nfaStack.push(createAnyCharNFA());
                }
                shouldConcatenate = true;
            }
            else if (Character.isLetterOrDigit(c)) {
                NFA charNFA = createSingleCharNFA(c);
                if (shouldConcatenate && !nfaStack.isEmpty()) {
                    NFA prev = nfaStack.pop();
                    nfaStack.push(concatenate(prev, charNFA));
                } else {
                    nfaStack.push(charNFA);
                }
                shouldConcatenate = true;
            } else if (c == '\\') {
                if (i + 1 < regex.length()) {
                    char next = regex.charAt(i + 1);
                    if (next == '.') {
                        NFA dotNFA = createSingleCharNFA('.');
                        if (shouldConcatenate && !nfaStack.isEmpty()) {
                            NFA prev = nfaStack.pop();
                            nfaStack.push(concatenate(prev, dotNFA));
                        } else {
                            nfaStack.push(dotNFA);
                        }
                        shouldConcatenate = true;
                        i++;
                    }
                }
            } else if (c == '[') {
                int end = regex.indexOf(']', i);
                if (end == -1) throw new IllegalArgumentException("Invalid character class");

                String charClass = regex.substring(i + 1, end);

                NFA charClassNFA;

                if ("+-*/%^".indexOf(charClass) != -1) {
                    charClassNFA = createCharacterClassNFA(charClass);
                } else {
                    charClassNFA = createCharacterClassNFA(charClass);
                }

                if (shouldConcatenate && !nfaStack.isEmpty()) {
                    NFA prev = nfaStack.pop();
                    nfaStack.push(concatenate(prev, charClassNFA));
                } else {
                    nfaStack.push(charClassNFA);
                }

                i = end;
                shouldConcatenate = true;
            }
            else if (c == '+') {
                if (nfaStack.isEmpty()) throw new IllegalArgumentException("Invalid `+` usage");
                NFA lastNFA = nfaStack.pop();
                NFA repeatedNFA = plus(lastNFA);
                nfaStack.push(repeatedNFA);
                shouldConcatenate = true;
            }
        }

        while (nfaStack.size() > 1) {
            NFA nfa2 = nfaStack.pop();
            NFA nfa1 = nfaStack.pop();
            nfaStack.push(concatenate(nfa1, nfa2));
        }

        NFA finalNFA = nfaStack.pop();

        if (startAnchored) {
            State newStart = new State(stateCounter++);
            newStart.addTransition('\0', finalNFA.startState);
            finalNFA = new NFA(newStart, finalNFA.finalState);
        }

        if (endAnchored) {
            finalNFA.finalState.isFinal = true;
        }

        return finalNFA;
    }

    public static NFA createAnyCharNFA() {
        State start = new State(stateCounter++);
        State end = new State(stateCounter++);
        for (char c = 32; c < 127; c++) { // All printable ASCII characters
            start.addTransition(c, end);
        }
        return new NFA(start, end);
    }

    private static void applyOperator(Stack<NFA> nfaStack, char operator) {
        if (operator == '|') {
            NFA nfa2 = nfaStack.pop();
            NFA nfa1 = nfaStack.pop();
            nfaStack.push(union(nfa1, nfa2));
        } else {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    public static NFA createSingleCharNFA(char c) {
        State start = new State(stateCounter++);
        State end = new State(stateCounter++);
        start.addTransition(c, end);
        return new NFA(start, end);
    }

    public static NFA createCharacterClassNFA(String charClass) {
        State start = new State(stateCounter++);
        State end = new State(stateCounter++);

        for (int i = 0; i < charClass.length(); i++) {
            char c = charClass.charAt(i);

            if (c == '\\' && i + 1 < charClass.length() && charClass.charAt(i + 1) == '-') {
                start.addTransition('-', end);
                i++;
            }
            else if (c == '-' && i > 0 && i < charClass.length() - 1) {
                char prev = charClass.charAt(i - 1);
                char next = charClass.charAt(i + 1);

                if (Character.isLetterOrDigit(prev) && Character.isLetterOrDigit(next) && prev < next) {
                    for (char ch = prev; ch <= next; ch++) {
                        start.addTransition(ch, end);
                    }
                    i++;
                } else {
                    start.addTransition('-', end);
                }
            }
            else {
                start.addTransition(c, end);
            }
        }

        return new NFA(start, end);
    }




    // Concatenation: Joins two NFAs sequentially (AB)
    public static NFA concatenate(NFA nfa1, NFA nfa2) {
        nfa1.finalState.isFinal = false;
        nfa1.finalState.addTransition('\0', nfa2.startState);
        return new NFA(nfa1.startState, nfa2.finalState);
    }

    // Union: Creates an NFA for A|B
    public static NFA union(NFA nfa1, NFA nfa2) {
        State start = new State(stateCounter++);
        State end = new State(stateCounter++);

        start.addTransition('\0', nfa1.startState);
        start.addTransition('\0', nfa2.startState);
        nfa1.finalState.addTransition('\0', end);
        nfa2.finalState.addTransition('\0', end);

        return new NFA(start, end);
    }

    private static int findMatchingParenthesis(String regex, int start) {
        int count = 0;
        for (int i = start; i < regex.length(); i++) {
            if (regex.charAt(i) == '(') count++;
            else if (regex.charAt(i) == ')') count--;
            if (count == 0) return i;
        }
        return -1;  // No match found
    }


    // Kleene Star: Creates an NFA for A*
    public static NFA kleeneStar(NFA nfa) {
        State start = new State(stateCounter++);
        State end = new State(stateCounter++);

        start.addTransition('\0', nfa.startState);
        start.addTransition('\0', end);
        nfa.finalState.addTransition('\0', nfa.startState);
        nfa.finalState.addTransition('\0', end);

        return new NFA(start, end);
    }

    // Plus operator (A+): At least one repetition
    public static NFA plus(NFA nfa) {
        // Ensure at least one repetition by concatenating nfa with its Kleene star
        return concatenate(nfa, kleeneStar(nfa));
    }

    // Optional (A?): Zero or one occurrence
    public static NFA optional(NFA nfa) {
        State start = new State(stateCounter++);
        State end = new State(stateCounter++);

        start.addTransition('\0', nfa.startState);
        start.addTransition('\0', end);
        nfa.finalState.addTransition('\0', end);

        return new NFA(start, end);
    }
}
