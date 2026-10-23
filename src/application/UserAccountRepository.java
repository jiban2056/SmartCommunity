package application;

import domain.UserAccount;
import java.util.Optional;

/**
 * Provides access to existing user accounts.
 * Implementations must return Optional.empty() when no account matches.
 */
public interface UserAccountRepository {

    Optional<UserAccount> findById(String accountId);

    Optional<UserAccount> findByEmail(String email);
}