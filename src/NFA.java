import java.util.HashSet;
import java.util.List;
import java.util.Set;

class NFA {
    State startState;
    State finalState;
    Set<State> allStates = new HashSet<>();
    String TOKEN_TYPE;
    // Stores all states in this NFA

    public NFA(State start, State end) {
        this.startState = start;
        this.finalState = end;
        end.isFinal = true;
        collectStates(start);  // Collect all states
    }


    private void collectStates(State state) {
        if (!allStates.contains(state)) {
            allStates.add(state);
            for (List<State> nextStates : state.transitions.values()) {
                for (State nextState : nextStates) {
                    collectStates(nextState);
                }
            }
        }
    }


    public void printTransitionTable() {
        System.out.println("Transition Table:");
        System.out.println("Start State: ");
        System.out.println(startState.id);
        System.out.println("End State: ");
        System.out.println(finalState.id);
        System.out.println();
        for (State state : allStates) {
            state.printTransitions();
        }
        System.out.println();
    }
}
