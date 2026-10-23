package domain;

import java.util.Set;

/**
 * Represents an account with one or more access roles.
 * Password verification is handled separately by authentication.
 */
public final class UserAccount implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String accountId;
    private final String fullName;
    private final String email;
    private final boolean verified;
    private final Set<Role> roles;

    public UserAccount(
            String accountId,
            String fullName,
            String email,
            boolean verified,
            Set<Role> roles) {

        this.accountId = requireText(accountId, "Account ID");
        this.fullName = requireText(fullName, "Full name");
        this.email = requireText(email, "Email");

        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException(
                    "An account must have at least one role.");
        }

        if (roles.stream().anyMatch(role -> role == null)) {
            throw new IllegalArgumentException(
                    "Roles must not contain null.");
        }

        this.verified = verified;
        this.roles = Set.copyOf(roles);
    }

    public String getAccountId() {
        return accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVerified() {
        return verified;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank.");
        }

        return value.trim();
    }
}