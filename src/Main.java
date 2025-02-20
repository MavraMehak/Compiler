import java.util.List;

public class Main {
    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        String code = "int x      = (5); bool y = true; char x = 'y'; // This is a comment int x\n int al = 5.5; /* multi line comment */";
        List<Token> tokens = lexer.tokenize(code);
        lexer.printSymbolTable();
    }
}
