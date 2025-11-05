package greencity.exception.exceptions;

public class DeleteFileException extends RuntimeException {
    public DeleteFileException(String message) {
        super(message);
    }

    public DeleteFileException(String message, Exception e) {
        super(message, e);
    }
}
