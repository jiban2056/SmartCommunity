package application;

import domain.UserAccount;
import java.util.Arrays;
import java.util.Objects;

/**
 * Authenticates verified accounts.
 * The supplied password array is cleared after every attempt.
 */
public final class AuthenticationService {

    private final UserAccountRepository accounts;
    private final PasswordVerifier passwords;

    public AuthenticationService(
            UserAccountRepository accounts,
            PasswordVerifier passwords) {

        this.accounts = Objects.requireNonNull(
                accounts, "Account repository is required.");
        this.passwords = Objects.requireNonNull(
                passwords, "Password verifier is required.");
    }

    public UserAccount login(String email, char[] suppliedPassword) {
        try {
            if (email == null || email.isBlank()
                    || suppliedPassword == null
                    || suppliedPassword.length == 0) {
                throw invalidCredentials();
            }

            UserAccount account = accounts.findByEmail(email.trim())
                    .orElseThrow(
                            AuthenticationService::invalidCredentials);

            if (!account.isVerified()
                    || !passwords.matches(
                            account.getAccountId(), suppliedPassword)) {
                throw invalidCredentials();
            }

            return account;

        } finally {
            if (suppliedPassword != null) {
                Arrays.fill(suppliedPassword, '\0');
            }
        }
    }

    private static IllegalArgumentException invalidCredentials() {
        return new IllegalArgumentException(
                "Invalid email or password, or account unavailable.");
    }
}
