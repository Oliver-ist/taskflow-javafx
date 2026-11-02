package com.taskflow.ui;

import com.taskflow.model.Priority;
import com.taskflow.model.Status;
import com.taskflow.model.Task;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

final class TaskDialog {

    private TaskDialog() {
    }

    static Optional<Task> show(Window owner, Task existing) {
        boolean editing = existing != null;

        Dialog<Task> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(editing ? "Edit Tugas" : "Tugas Baru");
        dialog.setHeaderText(editing ? "Ubah detail tugas" : "Isi detail tugas baru");
        DialogUtil.style(dialog.getDialogPane(), owner);

        ButtonType saveType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        TextField titleField = new TextField();
        titleField.setPromptText("Contoh: Menyelesaikan laporan praktikum");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Deskripsi (opsional)");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);

        ComboBox<Priority> priorityBox = new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
        ComboBox<Status> statusBox = new ComboBox<>(FXCollections.observableArrayList(Status.values()));
        DatePicker duePicker = new DatePicker();
        duePicker.setPromptText("Tanpa tenggat");
        duePicker.setEditable(false);

        if (editing) {
            titleField.setText(existing.getTitle());
            descArea.setText(existing.getDescription());
            priorityBox.setValue(existing.getPriority());
            statusBox.setValue(existing.getStatus());
            duePicker.setValue(existing.getDueDate());
        } else {
            priorityBox.setValue(Priority.MEDIUM);
            statusBox.setValue(Status.TODO);
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 8, 8, 8));
        ColumnConstraints labelCol = new ColumnConstraints();
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        titleField.setPrefWidth(340);
        priorityBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setMaxWidth(Double.MAX_VALUE);
        duePicker.setMaxWidth(Double.MAX_VALUE);

        grid.addRow(0, new Label("Judul *"), titleField);
        grid.addRow(1, new Label("Deskripsi"), descArea);
        grid.addRow(2, new Label("Prioritas"), priorityBox);
        grid.addRow(3, new Label("Status"), statusBox);
        grid.addRow(4, new Label("Tenggat"), duePicker);
        dialog.getDialogPane().setContent(grid);

        // Tombol Simpan nonaktif selama judul masih kosong.
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> titleField.getText().isBlank(), titleField.textProperty()));

        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            Task task = editing ? existing : new Task(titleField.getText().trim());
            task.setTitle(titleField.getText().trim());
            task.setDescription(descArea.getText().trim());
            task.setPriority(priorityBox.getValue());
            task.setStatus(statusBox.getValue());
            task.setDueDate(duePicker.getValue());
            return task;
        });

        return dialog.showAndWait();
    }
}
