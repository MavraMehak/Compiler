import java.util.*;

class State {
    int id;
    Map<Character, List<State>> transitions = new HashMap<>();
    boolean isFinal;

    public State(int id) {
        this.id = id;
        this.isFinal = false;
    }

    public void addTransition(char symbol, State nextState) {
        transitions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(nextState);
    }

    public void printTransitions() {
        for (Map.Entry<Character, List<State>> entry : transitions.entrySet()) {
            char symbol = entry.getKey();
            for (State nextState : entry.getValue()) {
                System.out.println("State " + id + " --[" + (symbol == '\0' ? "ε" : symbol) + "]--> State " + nextState.id);
            }
        }
    }
}
