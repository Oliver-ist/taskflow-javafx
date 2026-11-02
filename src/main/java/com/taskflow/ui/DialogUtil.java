package com.taskflow.ui;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

final class DialogUtil {

    private DialogUtil() {
    }

    static void style(DialogPane pane, Window owner) {
        if (owner == null || owner.getScene() == null) {
            return;
        }
        Scene scene = owner.getScene();
        pane.getStylesheets().addAll(scene.getStylesheets());
        if (scene.getRoot().getStyleClass().contains("dark")) {
            pane.getStyleClass().add("dark");
        }
    }

    static boolean confirm(Window owner, String title, String message, String confirmLabel) {
        ButtonType yes = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        style(alert.getDialogPane(), owner);
        return alert.showAndWait().filter(b -> b == yes).isPresent();
    }

    static void error(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message,
                new ButtonType("Tutup", ButtonBar.ButtonData.OK_DONE));
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        style(alert.getDialogPane(), owner);
        alert.showAndWait();
    }
}
