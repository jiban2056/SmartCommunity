import domain.*;
import infrastructure.FileServiceRequestRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Checks persistence using an isolated temporary directory.
 */
public class PersistenceCheck {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {

        Path temporaryFolder =
                Files.createTempDirectory("community-persistence-check-");
        Path storageFile = temporaryFolder.resolve("requests.bin");

        System.out.println("Test folder: " + temporaryFolder);

        FileServiceRequestRepository repository =
                new FileServiceRequestRepository(storageFile);

        check("Missing storage starts empty",
                repository.findAll().isEmpty());

        UserAccount resident = new UserAccount(
                "R01", "Resident", "resident@example.com",
                true, Set.of(Role.RESIDENT));

        UserAccount officer = new UserAccount(
                "C01", "Officer", "council@example.com",
                true, Set.of(Role.COUNCIL_EMPLOYEE));

        UserAccount worker = new UserAccount(
                "M01", "Worker", "worker@example.com",
                true, Set.of(Role.MAINTENANCE_STAFF));

        MaintenanceTeam team = new MaintenanceTeam("T01", "Road Team");
        team.addMember(worker);

        ServiceRequest request = new ServiceRequest(
                "REQ-SAVE-001",
                resident,
                "Damaged road surface",
                new RequestCategory("ROAD", "Road damage"),
                new Location("10 Example Street", -33.86, 151.20));

        request.assignTeam(
                officer, team, "Inspect and repair",
                LocalDateTime.now().plusDays(1));

        repository.save(request);

        check("Save creates a non-empty file",
                Files.exists(storageFile) && Files.size(storageFile) > 0);

        // A fresh repository simulates reopening the saved data.
        FileServiceRequestRepository reopened =
                new FileServiceRequestRepository(storageFile);

        ServiceRequest loaded = reopened
                .findByReference("REQ-SAVE-001")
                .orElseThrow(() -> new AssertionError("Request not restored"));

        check("Request details survive reload",
                loaded.getDescription().equals("Damaged road surface")
                        && loaded.getStatus() == RequestStatus.ASSIGNED
                        && loaded.getLocation().hasCoordinates()
                        && loaded.getCategory().getCategoryId().equals("ROAD"));

        check("Account roles and team membership survive reload",
                loaded.getSubmittedBy().hasRole(Role.RESIDENT)
                        && loaded.getActiveAssignment()
                            .getTeam().hasMember(worker));

        check("Assignment ID and due date survive reload",
                loaded.getActiveAssignment().getAssignmentId().equals(
                        request.getActiveAssignment().getAssignmentId())
                        && loaded.getActiveAssignment().getDueAt().equals(
                                request.getActiveAssignment().getDueAt()));

        check("Original history timestamps survive reload",
                loaded.getHistory().size() == request.getHistory().size()
                        && loaded.getHistory().get(0).getOccurredAt().equals(
                                request.getHistory().get(0).getOccurredAt()));

        loaded.startWork(worker);

        check("Unsaved changes do not change stored data",
                reopened.findByReference("REQ-SAVE-001")
                        .orElseThrow().getStatus() == RequestStatus.ASSIGNED);

        loaded.completeWork(
                worker,
                "Road repaired",
                new Evidence(
                        "sample/completion.jpg",
                        "Sample evidence reference",
                        worker,
                        LocalDateTime.now()));

        loaded.submitFeedback(resident, "Repair completed well");
        reopened.save(loaded);

        FileServiceRequestRepository finalRepository =
                new FileServiceRequestRepository(storageFile);

        ServiceRequest completed = finalRepository
                .findByReference("REQ-SAVE-001").orElseThrow();

        check("Updating a request does not create duplicates",
                finalRepository.findAll().size() == 1);

        check("Completed state and inactive assignment survive reload",
                completed.getStatus() == RequestStatus.COMPLETED
                        && completed.getActiveAssignment() == null
                        && completed.getAssignments().size() == 1);

        check("Evidence and feedback survive reload",
                completed.getEvidence().size() == 1
                        && completed.getEvidence().get(0).getFileReference()
                            .equals("sample/completion.jpg")
                        && completed.getFeedback().getComment()
                            .equals("Repair completed well"));

        check("Unknown reference returns empty",
                finalRepository.findByReference("MISSING").isEmpty());

        Path corruptFile = temporaryFolder.resolve("corrupt.bin");
        Files.writeString(corruptFile, "This is not serialized data");

        FileServiceRequestRepository corruptRepository =
                new FileServiceRequestRepository(corruptFile);

        expectStorageFailure("Corrupt storage is reported",
                () -> corruptRepository.findAll());

        expectStorageFailure("Save does not overwrite corrupt storage",
                () -> corruptRepository.save(request));

        check("Corrupt file remains unchanged",
                Files.readString(corruptFile)
                        .equals("This is not serialized data"));

        Files.delete(storageFile);
        Files.delete(corruptFile);
        Files.delete(temporaryFolder);

        System.out.println("\nAll " + passed
                + " persistence checks passed.");
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAIL: " + name);
        }

        passed++;
        System.out.println("PASS: " + name);
    }

    private static void expectStorageFailure(
            String name, Runnable action) {

        try {
            action.run();
        } catch (IllegalStateException expected) {
            passed++;
            System.out.println("PASS: " + name);
            return;
        }

        throw new AssertionError("FAIL: " + name);
    }
}