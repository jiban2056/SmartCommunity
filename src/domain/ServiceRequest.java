package domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controls the lifecycle and business rules of a service request.
 * Application services must supply the authenticated actor.
 */

public final class ServiceRequest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String reference;
    private final UserAccount submittedBy;
    private final String description;
    private final RequestCategory category;
    private final Location location;

    private RequestStatus status = RequestStatus.SUBMITTED;
    private Priority priority = Priority.NORMAL;
    private NotificationChannel preferredChannel = NotificationChannel.EMAIL;
    private boolean safetyRisk;

    private final List<TaskAssignment> assignments = new ArrayList<>();
    private final List<RequestHistoryEntry> history = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final List<Evidence> evidence = new ArrayList<>();

    private Feedback feedback;

    public ServiceRequest(
            String reference,
            UserAccount submittedBy,
            String description,
            RequestCategory category,
            Location location) {

        this.reference = requireText(reference, "Request reference");
        this.description = requireText(description, "Description");

        requireRole(submittedBy, Role.RESIDENT);

        if (category == null || location == null) {
            throw new IllegalArgumentException(
                    "Category and location are required.");
        }

        this.submittedBy = submittedBy;
        this.category = category;
        this.location = location;

        recordChange(submittedBy, "SUBMISSION", "", status.name());
    }

    public void changePriority(UserAccount actor, Priority newPriority) {
        requireRole(actor, Role.COUNCIL_EMPLOYEE);
        requireOpen();

        if (newPriority == null) {
            throw new IllegalArgumentException("Priority is required.");
        }

        if (safetyRisk && newPriority != Priority.URGENT) {
            throw new IllegalStateException(
                    "An immediate safety risk must remain urgent.");
        }

        if (priority != newPriority) {
            Priority previous = priority;
            priority = newPriority;
            recordChange(actor, "PRIORITY",
                    previous.name(), newPriority.name());
        }
    }

    public void escalateSafetyRisk(UserAccount actor, String reason) {
        requireRole(actor, Role.COUNCIL_EMPLOYEE);
        requireOpen();

        String explanation = requireText(reason, "Escalation reason");
        changePriority(actor, Priority.URGENT);
        safetyRisk = true;

        recordChange(actor, "SAFETY_ESCALATION", "", explanation);
    }

    public void assignTeam(
            UserAccount actor,
            MaintenanceTeam team,
            String instructions,
            LocalDateTime dueAt) {

        requireRole(actor, Role.COUNCIL_EMPLOYEE);
        requireOpen();

        // Proposed rule: reassign before work starts.
        if (status == RequestStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "An in-progress request cannot be reassigned.");
        }

        // Validate the replacement before changing the current assignment.
        TaskAssignment replacement = new TaskAssignment(
                UUID.randomUUID().toString(),
                reference,
                team,
                actor,
                instructions,
                LocalDateTime.now(),
                dueAt);

        TaskAssignment previous = getActiveAssignment();
        String previousTeam = "";

        if (previous != null) {
            previousTeam = previous.getTeam().getTeamId();
            previous.deactivate();
        }

        assignments.add(replacement);

        recordChange(actor, "ASSIGNMENT",
                previousTeam, team.getTeamId());

        if (status == RequestStatus.SUBMITTED) {
            transitionTo(actor, RequestStatus.ASSIGNED);
        }
    }

    public void startWork(UserAccount actor) {
        requireAssignedMember(actor);

        if (status != RequestStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Only an assigned request can start work.");
        }

        transitionTo(actor, RequestStatus.IN_PROGRESS);
    }

    public void addSubmissionEvidence(
            UserAccount actor, Evidence item) {

        requireRole(actor, Role.RESIDENT);

        if (!isSubmittedBy(actor)) {
            throw new IllegalArgumentException(
                    "Only the submitting resident can add this evidence.");
        }

        if (status != RequestStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Submission evidence must be added before assignment.");
        }

        requireEvidenceOwner(actor, item);
        evidence.add(item);

        recordChange(actor, "EVIDENCE_ADDED",
                "", item.getFileReference());
    }

    public void completeWork(
            UserAccount actor,
            String completionNote,
            Evidence completionEvidence) {

        requireAssignedMember(actor);

        if (status != RequestStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Work must be in progress before completion.");
        }

        String note = requireText(completionNote, "Completion note");
        requireEvidenceOwner(actor, completionEvidence);

        evidence.add(completionEvidence);
        recordChange(actor, "COMPLETION_EVIDENCE",
                "", completionEvidence.getFileReference());
        recordChange(actor, "COMPLETION_NOTE", "", note);

        TaskAssignment activeAssignment = getActiveAssignment();
        transitionTo(actor, RequestStatus.COMPLETED);
        activeAssignment.deactivate();
    }

    public void submitFeedback(UserAccount actor, String comment) {
        requireRole(actor, Role.RESIDENT);

        if (!isSubmittedBy(actor)) {
            throw new IllegalArgumentException(
                    "Only the submitting resident can provide feedback.");
        }

        if (status != RequestStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Feedback requires a completed request.");
        }

        if (feedback != null) {
            throw new IllegalStateException(
                    "Feedback has already been submitted.");
        }

        feedback = new Feedback(actor, comment, LocalDateTime.now());
        recordChange(actor, "FEEDBACK", "", "Feedback submitted");
    }

    public TaskAssignment getActiveAssignment() {
        for (TaskAssignment assignment : assignments) {
            if (assignment.isActive()) {
                return assignment;
            }
        }
        return null;
    }

    public boolean isSubmittedBy(UserAccount account) {
        return account != null
                && submittedBy.getAccountId().equals(
                        account.getAccountId());
    }

    private void requireAssignedMember(UserAccount actor) {
        requireRole(actor, Role.MAINTENANCE_STAFF);

        TaskAssignment assignment = getActiveAssignment();

        if (assignment == null
                || !assignment.getTeam().hasMember(actor)) {
            throw new IllegalArgumentException(
                    "Only members of the assigned team may update work.");
        }
    }

    private static void requireEvidenceOwner(
            UserAccount actor, Evidence item) {

        if (item == null) {
            throw new IllegalArgumentException("Evidence is required.");
        }

        if (!item.getUploadedBy().getAccountId()
                .equals(actor.getAccountId())) {
            throw new IllegalArgumentException(
                    "Evidence must belong to the acting user.");
        }
    }

    private static void requireRole(UserAccount actor, Role role) {
        if (actor == null || !actor.isVerified()
                || !actor.hasRole(role)) {
            throw new IllegalArgumentException(
                    "A verified account with role " + role + " is required.");
        }
    }

    private void requireOpen() {
        if (status == RequestStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed requests cannot be changed.");
        }
    }

    private void transitionTo(
            UserAccount actor, RequestStatus nextStatus) {

        RequestStatus previous = status;
        status = nextStatus;
        recordChange(actor, "STATUS", previous.name(), nextStatus.name());
    }

    private void recordChange(
            UserAccount actor,
            String type,
            String previous,
            String next) {

        history.add(new RequestHistoryEntry(
                LocalDateTime.now(), actor, type, previous, next));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank.");
        }
        return value.trim();
    }

    public String getReference() {
        return reference;
    }

    public UserAccount getSubmittedBy() {
        return submittedBy;
    }

    public String getDescription() {
        return description;
    }

    public RequestCategory getCategory() {
        return category;
    }

    public Location getLocation() {
        return location;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isSafetyRisk() {
        return safetyRisk;
    }

    public List<TaskAssignment> getAssignments() {
        return List.copyOf(assignments);
    }

    public List<RequestHistoryEntry> getHistory() {
        return List.copyOf(history);
    }

    public List<Evidence> getEvidence() {
        return List.copyOf(evidence);
    }

    public Feedback getFeedback() {
        return feedback;
    }

    /**
 * The submitting resident chooses a channel for this request.
 * EMAIL is the prototype default.
 */
public void changeNotificationChannel(
        UserAccount actor, NotificationChannel channel) {

    requireRole(actor, Role.RESIDENT);

    if (!isSubmittedBy(actor)) {
        throw new IllegalArgumentException(
                "Only the submitting resident can choose the channel.");
    }

    if (channel == null) {
        throw new IllegalArgumentException(
                "Notification channel is required.");
    }

    if (preferredChannel != channel) {
        NotificationChannel previous = preferredChannel;
        preferredChannel = channel;

        recordChange(actor, "NOTIFICATION_CHANNEL",
                previous.name(), channel.name());
    }
}

public NotificationChannel getPreferredChannel() {
    return preferredChannel;
}

/**
 * Stores the outcome of a notification attempt for this request.
 */
public void recordNotification(Notification notification) {
    if (notification == null) {
        throw new IllegalArgumentException(
                "Notification is required.");
    }

    if (!reference.equals(notification.getRequestReference())) {
        throw new IllegalArgumentException(
                "Notification belongs to another request.");
    }

    if (!submittedBy.getAccountId().equals(
            notification.getRecipient().getAccountId())) {
        throw new IllegalArgumentException(
                "Notification must target the submitting resident.");
    }

    if (notification.getChannel() != preferredChannel) {
        throw new IllegalArgumentException(
                "Notification must use the preferred channel.");
    }

    if (notification.getDeliveryStatus() == DeliveryStatus.PENDING) {
        throw new IllegalArgumentException(
                "Record a delivery outcome, not a pending notification.");
    }

    boolean duplicate = notifications.stream().anyMatch(
            existing -> existing.getNotificationId().equals(
                    notification.getNotificationId()));

    if (duplicate) {
        throw new IllegalArgumentException(
                "Notification has already been recorded.");
    }

    notifications.add(notification);
}

public List<Notification> getNotifications() {
    return List.copyOf(notifications);
}
}