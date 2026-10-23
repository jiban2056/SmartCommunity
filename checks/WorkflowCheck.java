import domain.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Runs focused checks without external testing libraries.
 * Uses temporary objects and does not change saved project data.
 */
public class WorkflowCheck {

    private static int passed = 0;

    public static void main(String[] args) {

        UserAccount resident = new UserAccount(
                "R01", "Demo Resident", "resident@example.com",
                true, Set.of(Role.RESIDENT));

        UserAccount otherResident = new UserAccount(
                "R02", "Other Resident", "other@example.com",
                true, Set.of(Role.RESIDENT));

        UserAccount officer = new UserAccount(
                "C01", "Council Officer", "council@example.com",
                true, Set.of(Role.COUNCIL_EMPLOYEE));

        UserAccount worker = new UserAccount(
                "M01", "Maintenance Worker", "worker@example.com",
                true, Set.of(Role.MAINTENANCE_STAFF));

        UserAccount outsider = new UserAccount(
                "M02", "Other Worker", "outsider@example.com",
                true, Set.of(Role.MAINTENANCE_STAFF));

        MaintenanceTeam team = new MaintenanceTeam("T01", "Road Team");
        team.addMember(worker);

        MaintenanceTeam replacementTeam =
                new MaintenanceTeam("T02", "Response Team");
        replacementTeam.addMember(worker);

        ServiceRequest request = new ServiceRequest(
                "REQ-001",
                resident,
                "Pothole near the community centre",
                new RequestCategory("ROAD", "Road damage"),
                new Location("10 Example Street"));

        check("Initial status is SUBMITTED",
                request.getStatus() == RequestStatus.SUBMITTED);

        check("Submission creates history",
                request.getHistory().size() == 1);

        expectRejected("Resident cannot change priority",
                () -> request.changePriority(resident, Priority.HIGH));

        expectRejected("Feedback before completion is blocked",
                () -> request.submitFeedback(resident, "Too early"));

        request.escalateSafetyRisk(officer, "Danger to passing vehicles");

        check("Safety escalation sets URGENT",
                request.isSafetyRisk()
                        && request.getPriority() == Priority.URGENT);

        expectRejected("Safety risk cannot be downgraded",
                () -> request.changePriority(officer, Priority.LOW));

        request.assignTeam(
                officer, team, "Inspect and repair",
                LocalDateTime.now().plusDays(1));

        check("Assignment changes status to ASSIGNED",
                request.getStatus() == RequestStatus.ASSIGNED);

        request.assignTeam(
                officer, replacementTeam, "Urgent inspection",
                LocalDateTime.now().plusHours(4));

        check("Reassignment keeps history and one active assignment",
                request.getAssignments().size() == 2
                        && !request.getAssignments().get(0).isActive()
                        && request.getAssignments().stream()
                            .filter(TaskAssignment::isActive).count() == 1);

        expectRejected("Invalid reassignment preserves current assignment",
                () -> request.assignTeam(
                        officer, team, "Invalid due date",
                        LocalDateTime.now().minusDays(1)));

        check("Current team remains unchanged after rejection",
                request.getActiveAssignment().getTeam().getTeamId()
                        .equals("T02"));

        expectRejected("Unassigned worker cannot start work",
                () -> request.startWork(outsider));

        request.startWork(worker);

        check("Assigned worker can start work",
                request.getStatus() == RequestStatus.IN_PROGRESS);

        expectRejected("Reassignment after starting is blocked",
                () -> request.assignTeam(
                        officer, team, "Move task",
                        LocalDateTime.now().plusDays(1)));

        expectRejected("Completion without evidence is blocked",
                () -> request.completeWork(worker, "Repaired", null));

        check("Rejected completion leaves request IN_PROGRESS",
                request.getStatus() == RequestStatus.IN_PROGRESS);

        Evidence evidence = new Evidence(
                "checks/sample-evidence.jpg",
                "Example completion evidence reference",
                worker,
                LocalDateTime.now());

        request.completeWork(worker, "Pothole repaired", evidence);

        check("Completion saves evidence and closes assignment",
                request.getStatus() == RequestStatus.COMPLETED
                        && request.getEvidence().size() == 1
                        && request.getActiveAssignment() == null);

        expectRejected("Another resident cannot submit feedback",
                () -> request.submitFeedback(otherResident, "Not my request"));

        request.submitFeedback(resident, "Thank you for the repair");

        check("Submitting resident can provide feedback",
                request.getFeedback() != null);

        expectRejected("Duplicate feedback is blocked",
                () -> request.submitFeedback(resident, "Second response"));

        check("History records actors and timestamps",
                request.getHistory().stream().allMatch(
                        entry -> entry.getActor() != null
                                && entry.getOccurredAt() != null));

        System.out.println("\nAll " + passed + " checks passed.");
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
        } catch (IllegalArgumentException | IllegalStateException expected) {
            passed++;
            System.out.println("PASS: " + name);
            return;
        }

        throw new AssertionError("FAIL: " + name);
    }
}