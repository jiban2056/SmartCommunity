package domain;

import java.time.LocalDateTime;

/**
 * Immutable notification record for the service request module.
 * Delivery adapters return an updated copy after an attempt.
 */
public final class Notification implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private final String notificationId;
    private final UserAccount recipient;
    private final String requestReference;
    private final NotificationChannel channel;
    private final String message;
    private final LocalDateTime createdAt;
    private final DeliveryStatus deliveryStatus;

    public Notification(
            String notificationId,
            UserAccount recipient,
            String requestReference,
            NotificationChannel channel,
            String message,
            LocalDateTime createdAt) {

        this(notificationId, recipient, requestReference, channel,
                message, createdAt, DeliveryStatus.PENDING);
    }

    private Notification(
            String notificationId,
            UserAccount recipient,
            String requestReference,
            NotificationChannel channel,
            String message,
            LocalDateTime createdAt,
            DeliveryStatus deliveryStatus) {

        this.notificationId = requireText(
                notificationId, "Notification ID");
        this.requestReference = requireText(
                requestReference, "Request reference");
        this.message = requireText(message, "Message");

        if (recipient == null || channel == null
                || createdAt == null || deliveryStatus == null) {
            throw new IllegalArgumentException(
                    "Recipient, channel, time and status are required.");
        }

        this.recipient = recipient;
        this.channel = channel;
        this.createdAt = createdAt;
        this.deliveryStatus = deliveryStatus;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public UserAccount getRecipient() {
        return recipient;
    }

    public String getRequestReference() {
        return requestReference;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public Notification withDeliveryStatus(DeliveryStatus status) {
        return new Notification(
                notificationId, recipient, requestReference,
                channel, message, createdAt, status);
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