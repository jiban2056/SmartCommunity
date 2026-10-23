package domain;

import java.time.LocalDateTime;

/**
 * Records a maintenance assignment.
 * ServiceRequest will enforce one active assignment per request.
 */
public final class TaskAssignment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String assignmentId;
    private final String requestReference;
    private final MaintenanceTeam team;
    private final UserAccount assignedBy;
    private final String instructions;
    private final LocalDateTime assignedAt;
    private final LocalDateTime dueAt;
    private boolean active;

    public TaskAssignment(
            String assignmentId,
            String requestReference,
            MaintenanceTeam team,
            UserAccount assignedBy,
            String instructions,
            LocalDateTime assignedAt,
            LocalDateTime dueAt) {

        this.assignmentId = requireText(
                assignmentId, "Assignment ID");
        this.requestReference = requireText(
                requestReference, "Request reference");
        this.instructions = requireText(
                instructions, "Instructions");

        if (team == null) {
            throw new IllegalArgumentException(
                    "Maintenance team is required.");
        }

        if (assignedBy == null
                || !assignedBy.isVerified()
                || !assignedBy.hasRole(Role.COUNCIL_EMPLOYEE)) {
            throw new IllegalArgumentException(
                    "Assignment requires a verified council employee.");
        }

        if (assignedAt == null || dueAt == null) {
            throw new IllegalArgumentException(
                    "Assignment time and due date are required.");
        }

        if (!dueAt.isAfter(assignedAt)) {
            throw new IllegalArgumentException(
                    "Due date must be after assignment time.");
        }

        this.team = team;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.dueAt = dueAt;
        this.active = true;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public String getRequestReference() {
        return requestReference;
    }

    public MaintenanceTeam getTeam() {
        return team;
    }

    public UserAccount getAssignedBy() {
        return assignedBy;
    }

    public String getInstructions() {
        return instructions;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public boolean isActive() {
        return active;
    }

    /*
     * Package-private: assignment lifecycle is managed
     * within the domain, rather than by the console UI.
     */
    void deactivate() {
        this.active = false;
    }

    private static String requireText(
            String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank.");
        }

        return value.trim();
    }
}