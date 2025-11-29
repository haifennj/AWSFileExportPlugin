package com.github.haifennj.ideaplugin.helper;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

public class NotificationUtil {

	private static void showNotification(AnActionEvent anActionEvent, String content, NotificationType notificationType) {
		ApplicationManager.getApplication().invokeLater(() -> {
			Notification notification = new Notification("AWS Developer Plugins Notification Group", "AWS Developer Plugins", content, notificationType);
			Notifications.Bus.notify(notification);
		});
	}

	public static void showInfoNotification(AnActionEvent anActionEvent, String content) {
		showNotification(anActionEvent, content, NotificationType.INFORMATION);
	}

	public static void showWarningNotification(AnActionEvent anActionEvent, String content) {
		showNotification(anActionEvent, content, NotificationType.WARNING);
	}

	public static void showErrorNotification(AnActionEvent anActionEvent, String content) {
		showNotification(anActionEvent, content, NotificationType.ERROR);
	}

	private static void showNotification(String content, NotificationType notificationType) {
		ApplicationManager.getApplication().invokeLater(() -> {
			Notification notification = new Notification("AWS Developer Plugins Notification Group", "AWS Developer Plugins", content, notificationType);
			Notifications.Bus.notify(notification);
		});
	}

	public static void showInfoNotification(Project project, String content) {
		showNotification(content, NotificationType.INFORMATION);
	}

	public static void showWarningNotification(Project project, String content) {
		showNotification(content, NotificationType.WARNING);
	}

	public static void showErrorNotification(Project project, String content) {
		showNotification(content, NotificationType.ERROR);
	}

	public static void info(String msg) {
		showNotification(null, msg, NotificationType.INFORMATION);
	}

	public static void warning(String msg) {
		showNotification(null, msg, NotificationType.WARNING);
	}

	public static void error(String msg) {
		showNotification(null, msg, NotificationType.ERROR);
	}
}
