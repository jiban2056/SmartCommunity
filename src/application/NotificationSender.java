package application;

import domain.Notification;

/**
 * Contract for sending a notification.
 * Implementations return the notification with its delivery result.
 */
public interface NotificationSender {

    Notification send(Notification notification);
}