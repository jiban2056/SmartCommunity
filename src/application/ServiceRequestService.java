package application;

import domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinates request operations for the authenticated user.
 * Notifications will be integrated separately.
 */
public final class ServiceRequestService {

    private final UserSession session;
    private final NotificationSender notificationSender;
    private final ServiceRequestRepository requests;
    private final EvidenceStorage evidenceStorage;
    private final Map<String, RequestCategory> categories;
    private final Map<String, MaintenanceTeam> teams;

    public ServiceRequestService(
        UserSession session,
        ServiceRequestRepository requests,
        EvidenceStorage evidenceStorage,
        Map<String, RequestCategory> categories,
        Map<String, MaintenanceTeam> teams,
        NotificationSender notificationSender) {

    this.session = Objects.requireNonNull(session);
    this.requests = Objects.requireNonNull(requests);
    this.evidenceStorage = Objects.requireNonNull(evidenceStorage);
    this.categories = Map.copyOf(categories);
    this.teams = Map.copyOf(teams);
    this.notificationSender = Objects.requireNonNull(notificationSender);
}

    public List<RequestCategory> listCategories() {
        session.requireUser();

        return categories.values().stream()
                .sorted((first, second) ->
                        first.getCategoryId().compareTo(second.getCategoryId()))
                .toList();
    }

    public List<MaintenanceTeam> listTeams() {
        session.requireRole(Role.COUNCIL_EMPLOYEE);

        return teams.values().stream()
                .sorted((first, second) ->
                        first.getTeamId().compareTo(second.getTeamId()))
                .toList();
    }

    public ServiceRequest submit(
        String description,
        String categoryId,
        String address,
        String optionalPhotoPath,
        NotificationChannel channel) {

        UserAccount actor = session.requireRole(Role.RESIDENT);
        RequestCategory category = categories.get(
                requireText(categoryId, "Category ID"));

        if (category == null) {
            throw new IllegalArgumentException("Unknown category.");
        }

        String reference;
        do {
            reference = "REQ-" + UUID.randomUUID();
        } while (requests.findByReference(reference).isPresent());

        ServiceRequest request = new ServiceRequest(
                reference, actor, description, category,
                new Location(address));
                request.changeNotificationChannel(actor, channel);  

        if (optionalPhotoPath != null && !optionalPhotoPath.isBlank()) {
            String path = optionalPhotoPath.trim();
            String lowerPath = path.toLowerCase(Locale.ROOT);

            if (!lowerPath.endsWith(".jpg")
                    && !lowerPath.endsWith(".jpeg")
                    && !lowerPath.endsWith(".png")) {
                throw new IllegalArgumentException(
                        "Submission photos must use JPG, JPEG or PNG.");
            }

            String storedReference = evidenceStorage.store(path);

            request.addSubmissionEvidence(actor, new Evidence(
                    storedReference,
                    "Resident submission photo",
                    actor,
                    LocalDateTime.now()));
        }

        saveWithNotification(request, "Your request has been submitted.");
        return request;
    }

    public ServiceRequest view(String reference) {
        UserAccount actor = session.requireUser();
        ServiceRequest request = load(reference);

        if (!canView(actor, request)) {
            throw new IllegalArgumentException(
                    "Access denied for this request.");
        }

        return request;
    }

    public List<ServiceRequest> listVisible(
            RequestStatus statusFilter,
            Priority priorityFilter,
            String categoryFilter) {

        UserAccount actor = session.requireUser();

        return requests.findAll().stream()
                .filter(request -> canView(actor, request))
                .filter(request -> statusFilter == null
                        || request.getStatus() == statusFilter)
                .filter(request -> priorityFilter == null
                        || request.getPriority() == priorityFilter)
                .filter(request -> categoryFilter == null
                        || categoryFilter.isBlank()
                        || request.getCategory().getCategoryId()
                            .equals(categoryFilter.trim()))
                .sorted((first, second) ->
                        first.getReference().compareTo(second.getReference()))
                .toList();
    }

    public ServiceRequest changePriority(
            String reference, Priority priority) {

        UserAccount actor = session.requireRole(Role.COUNCIL_EMPLOYEE);
        ServiceRequest request = load(reference);

        request.changePriority(actor, priority);
        requests.save(request);
        return request;
    }

    public ServiceRequest escalateSafetyRisk(
            String reference, String reason) {

        UserAccount actor = session.requireRole(Role.COUNCIL_EMPLOYEE);
        ServiceRequest request = load(reference);

        request.escalateSafetyRisk(actor, reason);
        requests.save(request);
        return request;
    }

    public ServiceRequest assign(
            String reference,
            String teamId,
            String instructions,
            LocalDateTime dueAt) {

        UserAccount actor = session.requireRole(Role.COUNCIL_EMPLOYEE);
        MaintenanceTeam team = teams.get(
                requireText(teamId, "Team ID"));

        if (team == null) {
            throw new IllegalArgumentException("Unknown maintenance team.");
        }

        ServiceRequest request = load(reference);
        request.assignTeam(actor, team, instructions, dueAt);

        saveWithNotification(request, "Your request has been assigned to a maintenance team.");
        return request;
    }

    public ServiceRequest startWork(String reference) {
        UserAccount actor = session.requireRole(Role.MAINTENANCE_STAFF);
        ServiceRequest request = load(reference);

        request.startWork(actor);
        saveWithNotification(request, "Work on your request has started.");
        return request;
    }

    public ServiceRequest completeWork(
            String reference,
            String completionNote,
            String evidencePath) {

        UserAccount actor = session.requireRole(Role.MAINTENANCE_STAFF);
        ServiceRequest request = load(reference);

        // Check permission and workflow before copying any file.
        TaskAssignment assignment = request.getActiveAssignment();

        if (assignment == null || !assignment.getTeam().hasMember(actor)) {
            throw new IllegalArgumentException(
                    "Only the assigned team may complete this request.");
        }

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Work must be in progress before completion.");
        }

        String note = requireText(completionNote, "Completion note");
        String path = requireText(evidencePath, "Evidence path");
        String storedReference = evidenceStorage.store(path);

        Evidence evidence = new Evidence(
                storedReference, note, actor, LocalDateTime.now());

        request.completeWork(actor, note, evidence);
        saveWithNotification(request, "Your request has been completed. You may now provide feedback.");
        return request;
    }

    public ServiceRequest submitFeedback(
            String reference, String comment) {

        UserAccount actor = session.requireRole(Role.RESIDENT);
        ServiceRequest request = load(reference);

        request.submitFeedback(actor, comment);
        requests.save(request);
        return request;
    }

    private ServiceRequest load(String reference) {
        String cleanReference = requireText(reference, "Request reference");

        return requests.findByReference(cleanReference)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Request not found."));
    }

    private boolean canView(UserAccount actor, ServiceRequest request) {
        if (actor.hasRole(Role.COUNCIL_EMPLOYEE)) {
            return true;
        }

        if (actor.hasRole(Role.RESIDENT) && request.isSubmittedBy(actor)) {
            return true;
        }

        if (actor.hasRole(Role.MAINTENANCE_STAFF)) {
            List<TaskAssignment> assignments = request.getAssignments();

            if (!assignments.isEmpty()) {
                // The latest assigned team can also view its completed work.
                TaskAssignment latest =
                        assignments.get(assignments.size() - 1);

                return latest.getTeam().hasMember(actor);
            }
        }

        return false;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank.");
        }

        return value.trim();
    }

    /**
 * Attempts notification delivery and saves its result with the request.
 * A delivery failure does not discard a valid workflow update.
 */
private void saveWithNotification(
        ServiceRequest request, String message) {

    Notification pending = new Notification(
            UUID.randomUUID().toString(),
            request.getSubmittedBy(),
            request.getReference(),
            request.getPreferredChannel(),
            message + " Reference: " + request.getReference(),
            LocalDateTime.now());

    Notification outcome;

    try {
        outcome = notificationSender.send(pending);

        if (outcome == null
                || outcome.getDeliveryStatus() == DeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Notification adapter returned no delivery outcome.");
        }
    } catch (RuntimeException deliveryFailure) {
        outcome = pending.withDeliveryStatus(DeliveryStatus.FAILED);
    }

    request.recordNotification(outcome);
    requests.save(request);
}
}