package infrastructure;

import application.NotificationSender;
import domain.DeliveryStatus;
import domain.Notification;

/**
 * Simulates email or SMS delivery without contacting a provider.
 */
public final class SimulatedNotificationSender
        implements NotificationSender {

    @Override
    public Notification send(Notification notification) {

        if (notification == null) {
            throw new IllegalArgumentException(
                    "Notification is required.");
        }

        if (notification.getDeliveryStatus() != DeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending notifications can be processed.");
        }

        // No real email or SMS is sent.
        return notification.withDeliveryStatus(
                DeliveryStatus.SIMULATED);
    }
}