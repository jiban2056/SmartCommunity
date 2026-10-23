package domain;

import java.time.LocalDateTime;

/**
 * An immutable record of a service request change.
 * Entries are owned by their ServiceRequest.
 */
public final class RequestHistoryEntry implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final LocalDateTime occurredAt;
    private final UserAccount actor;
    private final String changeType;
    private final String previousValue;
    private final String newValue;

    public RequestHistoryEntry(
            LocalDateTime occurredAt,
            UserAccount actor,
            String changeType,
            String previousValue,
            String newValue) {

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "Change time is required.");
        }

        if (actor == null) {
            throw new IllegalArgumentException(
                    "The account making the change is required.");
        }

        if (changeType == null || changeType.isBlank()) {
            throw new IllegalArgumentException(
                    "Change type must not be blank.");
        }

        if (newValue == null || newValue.isBlank()) {
            throw new IllegalArgumentException(
                    "New value must not be blank.");
        }

        this.occurredAt = occurredAt;
        this.actor = actor;
        this.changeType = changeType.trim();
        this.previousValue =
                previousValue == null ? "" : previousValue.trim();
        this.newValue = newValue.trim();
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public UserAccount getActor() {
        return actor;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }
}