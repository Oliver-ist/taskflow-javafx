package com.taskflow;

import com.taskflow.storage.TaskRepository;
import com.taskflow.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {

    private MainView mainView;

    @Override
    public void start(Stage stage) {
        TaskRepository repository = new TaskRepository(TaskRepository.defaultPath());
        mainView = new MainView(repository, stage);

        Scene scene = new Scene(mainView, 1040, 640);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/com/taskflow/styles.css")).toExternalForm());
        mainView.installShortcuts(scene);

        stage.setTitle("TaskFlow - Manajemen Tugas");
        stage.setMinWidth(900);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Pastikan perubahan terakhir tersimpan sebelum aplikasi benar-benar tertutup.
        if (mainView != null) {
            mainView.saveNow();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
