package utils;

import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;

public class Notification {

    /**
     * Notify - Real IDE native Notifications
     * @param title
     * @param message
     */
    public static void notify(String title, String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Notifications.Bus.notify(new com.intellij.notification.Notification("pra", title + ".", message + ".", NotificationType.INFORMATION));
            }
        });
    }

}