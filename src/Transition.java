class Transition {
    State from;
    State to;
    char symbol;

    public Transition(State from, State to, char symbol) {
        this.from = from;
        this.to = to;
        this.symbol = symbol;
    }
}