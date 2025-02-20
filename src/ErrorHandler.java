public class ErrorHandler {
    public void THROW_LEXICAL_ERROR(int idx, char c){
        throw new IllegalArgumentException("Unexpected character at index " + idx + ": " + c);
    }
}
