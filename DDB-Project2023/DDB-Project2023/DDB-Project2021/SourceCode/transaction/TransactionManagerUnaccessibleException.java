package transaction;

public class TransactionManagerUnaccessibleException extends Exception {
    public TransactionManagerUnaccessibleException() {
        super("Transaction Manager Unaccessible");
    }
}
