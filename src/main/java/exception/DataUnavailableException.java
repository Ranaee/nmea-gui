package exception;

public class DataUnavailableException extends RuntimeException {
    public DataUnavailableException(String message) {
        super(message);
    }
}
