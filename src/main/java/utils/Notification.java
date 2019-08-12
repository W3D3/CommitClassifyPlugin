package utils;

import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

import javax.swing.*;

public class Notification {

    /**
     * Notify - Real IDE native Notifications
     * @param title Title of notification
     * @param message Message body of the notification
     */
    public static void notify(String title, String message, NotificationType type) {
        SwingUtilities.invokeLater(() -> Notifications.Bus.notify(new com.intellij.notification.Notification("Commit Classifier", title, message, type)));
    }

    public static void notify(String title, String message) {
        notify(title, message, NotificationType.INFORMATION);
    }

    public static void notifyError(String title, String message) {
        notify(title, message, NotificationType.ERROR);
    }

}