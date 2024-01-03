package transaction;

public class InvalidIndexException extends Exception {
    public InvalidIndexException(String indexName) {
        super("Invalid index: " + indexName);
    }
}