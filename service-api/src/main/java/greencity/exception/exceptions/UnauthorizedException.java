package greencity.exception.exceptions;

/**
 * Exception that we get when user trying to execute operation without proper
 * authority.
 *
 * @author Oleksandr Braiko
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
