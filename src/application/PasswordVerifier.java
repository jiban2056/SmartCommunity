package application;

/**
 * Checks a supplied password against stored credentials.
 * Implementations must not log or retain the supplied password.
 */
public interface PasswordVerifier {

    boolean matches(String accountId, char[] suppliedPassword);
}