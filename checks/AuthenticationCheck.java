import application.AuthenticationService;
import domain.Role;
import domain.UserAccount;
import infrastructure.DemoAccountStore;

/**
 * Checks demo authentication without changing stored project data.
 */
public class AuthenticationCheck {

    private static int passed = 0;

    public static void main(String[] args) {
        DemoAccountStore store = new DemoAccountStore();
        AuthenticationService authentication =
                new AuthenticationService(store, store);

        char[] password = "Resident123!".toCharArray();
        UserAccount resident = authentication.login(
                "resident@example.com", password);

        check("Correct credentials allow login",
                resident.getAccountId().equals("R01"));

        check("Resident has the expected role",
                resident.hasRole(Role.RESIDENT)
                        && !resident.hasRole(Role.COUNCIL_EMPLOYEE));

        check("Password array is cleared after success",
                isCleared(password));

        UserAccount council = authentication.login(
                "  COUNCIL@EXAMPLE.COM  ",
                "Council123!".toCharArray());

        check("Email matching ignores case and surrounding spaces",
                council.getAccountId().equals("C01")
                        && council.hasRole(Role.COUNCIL_EMPLOYEE));

        char[] wrongPassword = "WrongPassword!".toCharArray();

        expectRejected("Incorrect password is blocked",
                () -> authentication.login(
                        "resident@example.com", wrongPassword));

        check("Password array is cleared after failure",
                isCleared(wrongPassword));

        expectRejected("Unknown email is blocked",
                () -> authentication.login(
                        "unknown@example.com",
                        "Resident123!".toCharArray()));

        expectRejected("Blank email is blocked",
                () -> authentication.login(
                        " ", "Resident123!".toCharArray()));

        expectRejected("Empty password is blocked",
                () -> authentication.login(
                        "resident@example.com", new char[0]));

        expectRejected("Null password is blocked",
                () -> authentication.login(
                        "resident@example.com", null));

        System.out.println("\nAll " + passed
                + " authentication checks passed.");
    }

    private static boolean isCleared(char[] password) {
        for (char character : password) {
            if (character != '\0') {
                return false;
            }
        }
        return true;
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAIL: " + name);
        }

        passed++;
        System.out.println("PASS: " + name);
    }

    private static void expectRejected(String name, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            check(name, "Invalid email or password, or account unavailable."
                    .equals(expected.getMessage()));
            return;
        }

        throw new AssertionError("FAIL: " + name);
    }
}