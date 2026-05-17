package com.nuapps.ivping.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public final class AlertUtils {

    private AlertUtils() {
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}