import java.util.*;

class DFA {
    private State startState;
    private Set<State> allStates;
    private Map<Set<State>, State> stateMapping;
    private int localStateCounter = 0;
    String TOKEN_TYPE;

    public DFA(NFA nfa) {
        this.allStates = new HashSet<>();
        this.stateMapping = new HashMap<>();
        convertNFAtoDFA(nfa);
    }

    private void convertNFAtoDFA(NFA nfa) {
        Queue<Set<State>> queue = new LinkedList<>();
        Set<State> startClosure = epsilonClosure(new HashSet<>(Collections.singleton(nfa.startState)));


        startState = getStateForSet(startClosure);

        queue.add(startClosure);

        while (!queue.isEmpty()) {
            Set<State> currentSet = queue.poll();
            State dfaState = getStateForSet(currentSet);

            Map<Character, Set<State>> transitions = new HashMap<>();
            for (State state : currentSet) {
                for (char symbol : state.transitions.keySet()) {
                    if (symbol != '\0') { // Ignore epsilon transitions
                        transitions.putIfAbsent(symbol, new HashSet<>());
                        for (State nextState : state.transitions.get(symbol)) {
                            transitions.get(symbol).addAll(epsilonClosure(new HashSet<>(Collections.singleton(nextState))));
                        }
                    }
                }
            }

            for (char symbol : transitions.keySet()) {
                Set<State> targetSet = transitions.get(symbol);
                if (!stateMapping.containsKey(targetSet)) {
                    queue.add(targetSet);
                }
                State targetDFAState = getStateForSet(targetSet);
                dfaState.addTransition(symbol, targetDFAState);
            }
        }
    }

    public boolean isAccepted(String input) {
        if (startState == null) {
            System.err.println("Error: DFA start state is not set.");
            return false;
        }

        State currentState = startState;

        for (char symbol : input.toCharArray()) {
            if (!currentState.transitions.containsKey(symbol)) {
                return false; // Invalid character transition
            }
            currentState = currentState.transitions.get(symbol).get(0); // Move to next state
        }

        return currentState.isFinal; // Accept if in a final state
    }


    private Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        stack.addAll(states);

        while (!stack.isEmpty()) {
            State state = stack.pop();
            if (state.transitions.containsKey('\0')) {
                for (State epsilonState : state.transitions.get('\0')) {
                    if (!closure.contains(epsilonState)) {
                        closure.add(epsilonState);
                        stack.push(epsilonState);
                    }
                }
            }
        }
        return closure;
    }

    private State getStateForSet(Set<State> nfaStates) {
        return stateMapping.computeIfAbsent(nfaStates, set -> {
            State newState = new State(localStateCounter++);
            newState.isFinal = set.stream().anyMatch(s -> s.isFinal);
            allStates.add(newState);
            return newState;
        });
    }

    public void minimize() {
        Set<Set<State>> partitions = new HashSet<>();
        Set<State> finalStates = new HashSet<>();
        Set<State> nonFinalStates = new HashSet<>();

        for (State state : allStates) {
            if (state.isFinal) {
                finalStates.add(state);
            } else {
                nonFinalStates.add(state);
            }
        }
        if (!finalStates.isEmpty()) partitions.add(finalStates);
        if (!nonFinalStates.isEmpty()) partitions.add(nonFinalStates);

        boolean changed;
        do {
            changed = false;
            Set<Set<State>> newPartitions = new HashSet<>();

            for (Set<State> partition : partitions) {
                Map<Map<Character, State>, Set<State>> transitionGroups = new HashMap<>();

                for (State state : partition) {
                    Map<Character, State> transitions = new HashMap<>();
                    for (char symbol : state.transitions.keySet()) {
                        transitions.put(symbol, state.transitions.get(symbol).get(0));
                    }

                    transitionGroups.computeIfAbsent(transitions, k -> new HashSet<>()).add(state);
                }

                newPartitions.addAll(transitionGroups.values());
                if (transitionGroups.size() > 1) changed = true;
            }

            partitions = newPartitions;
        } while (changed);

        Map<State, State> stateMapping = new HashMap<>();
        for (Set<State> partition : partitions) {
            State representative = partition.iterator().next();
            for (State state : partition) {
                stateMapping.put(state, representative);
            }
        }

        for (State state : allStates) {
            Map<Character, List<State>> updatedTransitions = new HashMap<>();
            for (Map.Entry<Character, List<State>> entry : state.transitions.entrySet()) {
                updatedTransitions.put(entry.getKey(), List.of(stateMapping.get(entry.getValue().get(0))));
            }
            state.transitions = updatedTransitions;
        }

        allStates = new HashSet<>(stateMapping.values());
        startState = stateMapping.get(startState);
    }

    public MatchResult match(String input) {
        State currentState = startState;
        int matchLength = 0;  // Length of the longest accepted token
        int i = 0;

        for (; i < input.length(); i++) {
            char symbol = input.charAt(i);

            if (!currentState.transitions.containsKey(symbol)) {
                break; // Stop at the first invalid transition
            }

            currentState = currentState.transitions.get(symbol).get(0); // Move to next state

            if (currentState.isFinal) {
                matchLength = i + 1; // Update match length when reaching a final state
            }
        }

        return new MatchResult(matchLength > 0, matchLength);
    }

    public void printTransitionTable() {
        System.out.println("DFA Transition Table:");
        System.out.println("Start State: ");
        System.out.println(startState.id);
        System.out.print("Final States: ");
        for (State state : allStates) {
            if (state.isFinal)
                System.out.print(state.id + " ");
        }
        System.out.println();
        for (State state : allStates) {
            state.printTransitions();
        }
    }
}
