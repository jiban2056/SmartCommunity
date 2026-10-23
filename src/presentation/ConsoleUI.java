package presentation;

import application.ServiceRequestService;
import application.UserSession;
import domain.*;

import java.io.Console;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Console input and output only.
 * Application services enforce permissions and coordinate operations.
 */
public final class ConsoleUI {

    private final UserSession session;
    private final ServiceRequestService service;
    private final Scanner input = new Scanner(System.in);

    public ConsoleUI(
            UserSession session,
            ServiceRequestService service) {

        this.session = session;
        this.service = service;
    }

    public void run() {
        System.out.println("\nSMART COMMUNITY SERVICES");
        System.out.println("Local prototype | Email/SMS delivery is simulated.");

        while (true) {
            try {
                if (!session.isLoggedIn()) {
                    String choice = read("\n1. Login | 0. Exit\nChoose: ");

                    if (choice.equals("0")) {
                        return;
                    }

                    if (!choice.equals("1")) {
                        System.out.println("Choose 1 or 0.");
                        continue;
                    }

                    login();
                    continue;
                }

                showMenu();
                String choice = read("Choose: ");

                if (choice.equals("0")) {
                    session.logout();
                    return;
                }

                if (choice.equals("L")) {
                    session.logout();
                    System.out.println("Logged out.");
                    continue;
                }

                handleAction(choice);

            } catch (NoSuchElementException endOfInput) {
                session.logout();
                return;

            } catch (DateTimeParseException exception) {
                System.out.println(
                        "Use date/time format yyyy-MM-ddTHH:mm.");

            } catch (IllegalArgumentException | IllegalStateException exception) {
                System.out.println("Action not completed: "
                        + exception.getMessage());
            }
        }
    }

    private void login() {
        String email = read("Email: ");
        char[] password;

        Console console = System.console();

        if (console != null) {
            password = console.readPassword("Password: ");
        } else {
            System.out.println(
                    "Password input is visible in this terminal.");
            // Preserve password spaces rather than trimming them.
            System.out.print("Password: ");
            password = input.nextLine().toCharArray();
        }

        UserAccount user = session.login(email, password);
        System.out.println("Welcome, " + user.getFullName()
                + " | Roles: " + user.getRoles());
    }

    private void showMenu() {
        UserAccount user = session.requireUser();

        System.out.println("\n--- MENU ---");
        System.out.println("1. List/filter visible requests");
        System.out.println("2. View request details");

        if (user.hasRole(Role.RESIDENT)) {
            System.out.println("3. Submit request");
            System.out.println("8. Submit feedback");
        }

        if (user.hasRole(Role.COUNCIL_EMPLOYEE)) {
            System.out.println("4. Change priority");
            System.out.println("5. Assign/reassign team");
            System.out.println("9. Escalate safety risk");
        }

        if (user.hasRole(Role.MAINTENANCE_STAFF)) {
            System.out.println("6. Start work");
            System.out.println("7. Complete work with evidence");
        }

        System.out.println("L. Logout | 0. Exit");
    }

    private void handleAction(String choice) {
        switch (choice.toUpperCase(Locale.ROOT)) {
            case "1" -> listRequests();
            case "2" -> showDetails(
                    service.view(read("Request reference: ")));
            case "3" -> submitRequest();

            case "4" -> {
                String reference = read("Request reference: ");
                Priority priority = requiredEnum(
                        Priority.class, "Priority");
                showSaved(service.changePriority(reference, priority));
            }

            case "5" -> assignRequest();

            case "6" -> showSaved(
                    service.startWork(read("Request reference: ")));

            case "7" -> {
                String reference = read("Request reference: ");
                String note = read("Completion note: ");
                String path = readPath("Evidence file path: ");

                showSaved(service.completeWork(reference, note, path));
            }

            case "8" -> {
                String reference = read("Request reference: ");
                String comment = read("Feedback: ");

                showSaved(service.submitFeedback(reference, comment));
            }

            case "9" -> {
                String reference = read("Request reference: ");
                String reason = read("Immediate safety risk/reason: ");

                showSaved(service.escalateSafetyRisk(reference, reason));
            }

            default -> System.out.println("Unknown menu option.");
        }
    }

    private void submitRequest() {
        System.out.println("\nAvailable categories:");

        for (RequestCategory category : service.listCategories()) {
            System.out.println(
                    category.getCategoryId() + " - " + category.getName());
        }

        String category = read("Category ID: ").toUpperCase(Locale.ROOT);
        String description = read("Issue description: ");
        String address = read("Address/location: ");
        String photo = readPath("Photo path (Enter to skip): ");
        NotificationChannel channel = requiredEnum(
                NotificationChannel.class, "Preferred channel");

        ServiceRequest request = service.submit(
                description, category, address, photo, channel);

        showSaved(request);
    }

    private void assignRequest() {
        // Service checks council permissions before revealing teams.
        for (MaintenanceTeam team : service.listTeams()) {
            System.out.println(team.getTeamId() + " - " + team.getName());
        }

        String reference = read("Request reference: ");
        String teamId = read("Team ID: ").toUpperCase(Locale.ROOT);
        String instructions = read("Work instructions: ");
        LocalDateTime dueAt = LocalDateTime.parse(
                read("Future due date/time (yyyy-MM-ddTHH:mm): "));

        showSaved(service.assign(
                reference, teamId, instructions, dueAt));
    }

    private void listRequests() {
        RequestStatus status = optionalEnum(
                RequestStatus.class, "Status");
        Priority priority = optionalEnum(Priority.class, "Priority");
        String category = read(
                "Category ID (Enter for all): ").toUpperCase(Locale.ROOT);

        List<ServiceRequest> requests =
                service.listVisible(status, priority, category);

        if (requests.isEmpty()) {
            System.out.println("No matching requests available to you.");
            return;
        }

        for (ServiceRequest request : requests) {
            System.out.println(
                    request.getReference() + " | "
                    + request.getStatus() + " | "
                    + request.getPriority() + " | "
                    + request.getCategory().getName() + " | "
                    + request.getDescription());
        }
    }

    private void showSaved(ServiceRequest request) {
        System.out.println("\nSaved successfully.");
        System.out.println("Reference: " + request.getReference());
        System.out.println("Status: " + request.getStatus());
        System.out.println("Use option 2 to view history and notifications.");
    }

    private void showDetails(ServiceRequest request) {
        System.out.println("\n--- REQUEST DETAILS ---");
        System.out.println("Reference: " + request.getReference());
        System.out.println("Description: " + request.getDescription());
        System.out.println("Category: " + request.getCategory().getName());
        System.out.println("Location: " + request.getLocation().getAddress());
        System.out.println("Status: " + request.getStatus());
        System.out.println("Priority: " + request.getPriority());
        System.out.println("Safety risk: " + request.isSafetyRisk());
        System.out.println("Preferred channel: "
                + request.getPreferredChannel());

        System.out.println("\nASSIGNMENTS");
        for (TaskAssignment assignment : request.getAssignments()) {
            System.out.println(
                    assignment.getTeam().getName()
                    + " | Due: " + assignment.getDueAt()
                    + " | Active: " + assignment.isActive()
                    + " | Instructions: " + assignment.getInstructions());
        }

        System.out.println("\nHISTORY");
        for (RequestHistoryEntry entry : request.getHistory()) {
            System.out.println(
                    entry.getOccurredAt()
                    + " | " + entry.getActor().getAccountId()
                    + " | " + entry.getChangeType()
                    + " | " + entry.getPreviousValue()
                    + " -> " + entry.getNewValue());
        }

        System.out.println("\nEVIDENCE");
        for (Evidence evidence : request.getEvidence()) {
            System.out.println(
                    evidence.getDescription()
                    + " | Stored file: data/evidence/"
                    + evidence.getFileReference());
        }

        System.out.println("\nNOTIFICATIONS");
        for (Notification notification : request.getNotifications()) {
            System.out.println(
                    notification.getCreatedAt()
                    + " | " + notification.getChannel()
                    + " | " + notification.getDeliveryStatus()
                    + " | Recipient: "
                    + notification.getRecipient().getAccountId()
                    + " | " + notification.getMessage());
        }

        if (request.getFeedback() != null) {
            System.out.println("\nFEEDBACK: "
                    + request.getFeedback().getComment());
        }
    }

    private String read(String prompt) {
        System.out.print(prompt);
        return input.nextLine().trim();
    }

    private String readPath(String prompt) {
        String path = read(prompt);

        // Accept Windows "Copy as path" values with surrounding quotes.
        if (path.length() >= 2
                && path.startsWith("\"")
                && path.endsWith("\"")) {
            return path.substring(1, path.length() - 1);
        }

        return path;
    }

    private <E extends Enum<E>> E requiredEnum(
            Class<E> type, String label) {

        E value = optionalEnum(type, label);

        if (value == null) {
            throw new IllegalArgumentException(label + " is required.");
        }

        return value;
    }

    private <E extends Enum<E>> E optionalEnum(
            Class<E> type, String label) {

        System.out.println(label + " options: "
                + java.util.Arrays.toString(type.getEnumConstants()));

        String value = read(label + " (Enter to leave blank): ");

        if (value.isBlank()) {
            return null;
        }

        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}