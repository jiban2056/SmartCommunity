import application.*;
import domain.*;
import infrastructure.*;
import presentation.ConsoleUI;

import java.nio.file.Path;
import java.util.Map;

/**
 * Composition root: creates and connects application dependencies.
 */
public final class Main {

    public static void main(String[] args) {
        try {
            DemoAccountStore accounts = new DemoAccountStore();

            AuthenticationService authentication =
                    new AuthenticationService(accounts, accounts);

            UserSession session = new UserSession(authentication);

            MaintenanceTeam roadTeam =
                    new MaintenanceTeam("T01", "Road Maintenance");

            roadTeam.addMember(
                    accounts.findById("M01").orElseThrow());

            MaintenanceTeam generalTeam =
                    new MaintenanceTeam("T02", "General Maintenance");

            generalTeam.addMember(
                    accounts.findById("M02").orElseThrow());

            Map<String, MaintenanceTeam> teams = Map.of(
                    "T01", roadTeam,
                    "T02", generalTeam);

            Map<String, RequestCategory> categories = Map.of(
                    "ROAD", new RequestCategory("ROAD", "Road damage"),
                    "WASTE", new RequestCategory("WASTE", "Waste collection"),
                    "GRAFFITI", new RequestCategory("GRAFFITI", "Graffiti"),
                    "LIGHT", new RequestCategory("LIGHT", "Street lighting"));

            ServiceRequestRepository requests =
                    new FileServiceRequestRepository(
                            Path.of("data", "requests.bin"));

            EvidenceStorage evidenceStorage =
                    new LocalEvidenceStorage(
                            Path.of("data", "evidence"));

            NotificationSender notifications =
                    new SimulatedNotificationSender();

            ServiceRequestService service = new ServiceRequestService(
                    session,
                    requests,
                    evidenceStorage,
                    categories,
                    teams,
                    notifications);

            new ConsoleUI(session, service).run();

        } catch (RuntimeException exception) {
            System.err.println(
                    "Application stopped: " + exception.getMessage());
        }
    }
}