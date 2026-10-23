package infrastructure;

import application.PasswordVerifier;
import application.UserAccountRepository;
import domain.Role;
import domain.UserAccount;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Supplies fixed demo accounts and verifies salted password hashes.
 * Accounts are recreated with stable IDs at application startup.
 * This is not a production account-management system.
 */
public final class DemoAccountStore
        implements UserAccountRepository, PasswordVerifier {

    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;

    private final Map<String, UserAccount> accountsById = new HashMap<>();
    private final Map<String, UserAccount> accountsByEmail = new HashMap<>();
    private final Map<String, byte[]> salts = new HashMap<>();
    private final Map<String, byte[]> passwordHashes = new HashMap<>();

    private final SecureRandom random = new SecureRandom();

    public DemoAccountStore() {
        addAccount(
                "R01", "Demo Resident", "resident@example.com",
                Role.RESIDENT, "Resident123!".toCharArray());

        addAccount(
                "R02", "Other Resident", "other@example.com",
                Role.RESIDENT, "Other123!".toCharArray());

        addAccount(
                "C01", "Council Officer", "council@example.com",
                Role.COUNCIL_EMPLOYEE, "Council123!".toCharArray());

        addAccount(
                "M01", "Maintenance Worker", "worker@example.com",
                Role.MAINTENANCE_STAFF, "Worker123!".toCharArray());

        addAccount(
                "M02", "Other Worker", "outsider@example.com",
                Role.MAINTENANCE_STAFF, "OtherWorker123!".toCharArray());
    }

    private void addAccount(
            String id,
            String name,
            String email,
            Role role,
            char[] password) {

        try {
            UserAccount account = new UserAccount(
                    id, name, email, true, Set.of(role));

            byte[] salt = new byte[16];
            random.nextBytes(salt);
            byte[] hash = deriveHash(password, salt);

            accountsById.put(id, account);
            accountsByEmail.put(normaliseEmail(email), account);
            salts.put(id, salt);
            passwordHashes.put(id, hash);

        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Override
    public Optional<UserAccount> findById(String accountId) {
        if (accountId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                accountsById.get(accountId.trim()));
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                accountsByEmail.get(normaliseEmail(email)));
    }

    @Override
    public boolean matches(String accountId, char[] suppliedPassword) {
        if (accountId == null || suppliedPassword == null
                || suppliedPassword.length == 0) {
            return false;
        }

        byte[] salt = salts.get(accountId);
        byte[] expectedHash = passwordHashes.get(accountId);

        if (salt == null || expectedHash == null) {
            return false;
        }

        byte[] actualHash = deriveHash(suppliedPassword, salt);

        try {
            return MessageDigest.isEqual(expectedHash, actualHash);
        } finally {
            Arrays.fill(actualHash, (byte) 0);
        }
    }

    private static byte[] deriveHash(char[] password, byte[] salt) {
        PBEKeySpec specification = new PBEKeySpec(
                password, salt, ITERATIONS, KEY_LENGTH);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(specification).getEncoded();

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Password verification could not be initialised.",
                    exception);
        } finally {
            specification.clearPassword();
        }
    }

    private static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}