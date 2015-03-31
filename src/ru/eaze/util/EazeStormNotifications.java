package ru.eaze.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.project.Project;

public enum EazeStormNotifications {

    MISSING_SITES_CONFIG(MissingSitesConfigNotification.class),
    PAGES_CACHE_CHECKSUM_MISMATCH(PagesCacheChecksumMismatchNotification.class);

    private final static String GROUP_ID = "EazeStorm";

    private final Class<? extends Notification> type;

    EazeStormNotifications(Class<? extends Notification> type) {
        this.type = type;
    }

    public void show(Project project) {
        try {
            Notification notification = type.newInstance();
            Notifications.Bus.notify(notification, project);
        } catch (Exception ignored) {
        }
    }

    public void expire(Project project) {
        Notification[] notifications = NotificationsManager.getNotificationsManager().getNotificationsOfType(type, project);
        for (Notification notification : notifications) {
            notification.expire();
        }
    }

    private static class MissingSitesConfigNotification extends Notification {
        MissingSitesConfigNotification() {
            super(GROUP_ID, "Eaze project error",
                    "Missing file etc/conf/sites.xml in web directory",
                    NotificationType.ERROR);
        }
    }

    private static class PagesCacheChecksumMismatchNotification extends Notification {
        PagesCacheChecksumMismatchNotification() {
            super(GROUP_ID, "Obsolete pages.xml cache",
                    "Pages cache does not match current pages.xml checksum. Update cache or disable checksum verification in project settings.",
                    NotificationType.INFORMATION);
        }
    }
}
