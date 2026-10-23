package application;

import domain.Role;
import domain.UserAccount;
import java.util.Objects;

/**
 * Holds the authenticated user for this single-user console session.
 * Only successful authentication establishes a session.
 */
public final class UserSession {

    private final AuthenticationService authentication;
    private UserAccount currentUser;

    public UserSession(AuthenticationService authentication) {
        this.authentication = Objects.requireNonNull(
                authentication, "Authentication service is required.");
    }

    public UserAccount login(String email, char[] password) {
        // A failed new login must not retain the previous user's session.
        currentUser = null;
        currentUser = authentication.login(email, password);
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public UserAccount requireUser() {
        if (currentUser == null) {
            throw new IllegalStateException(
                    "Please log in before performing this action.");
        }

        return currentUser;
    }

    public UserAccount requireRole(Role role) {
        Objects.requireNonNull(role, "Required role is missing.");

        UserAccount user = requireUser();

        if (!user.hasRole(role)) {
            throw new IllegalArgumentException(
                    "Access denied. Required role: " + role);
        }

        return user;
    }
}