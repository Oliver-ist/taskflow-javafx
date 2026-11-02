package com.taskflow.ui;

import com.taskflow.model.Priority;
import com.taskflow.model.Status;
import com.taskflow.model.Task;
import com.taskflow.storage.TaskRepository;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

public class MainView extends BorderPane {

    private static final String ALL_STATUS = "Semua status";
    private static final String ALL_PRIORITY = "Semua prioritas";
    private static final String PREF_DARK = "darkMode";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TaskRepository repository;
    private final Stage stage;
    private final Preferences prefs = Preferences.userNodeForPackage(MainView.class);

    private final ObservableList<Task> tasks = FXCollections.observableArrayList(task -> new Observable[]{
            task.titleProperty(), task.descriptionProperty(), task.priorityProperty(),
            task.statusProperty(), task.dueDateProperty()
    });
    private final FilteredList<Task> filtered = new FilteredList<>(tasks, t -> true);
    private final SortedList<Task> sorted = new SortedList<>(filtered);
    private final TableView<Task> table = new TableView<>(sorted);

    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> priorityFilter = new ComboBox<>();
    private final Label statsLabel = new Label();
    private final Label shownLabel = new Label();
    private final Label saveLabel = new Label("Belum ada perubahan");
    private final Label progressLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final PauseTransition saveDelay = new PauseTransition(Duration.millis(400));

    public MainView(TaskRepository repository, Stage stage) {
        this.repository = repository;
        this.stage = stage;
        getStyleClass().add("main-view");

        sorted.comparatorProperty().bind(table.comparatorProperty());
        saveDelay.setOnFinished(e -> saveNow());

        Node center = buildTable();
        setTop(buildToolbar());
        setCenter(center);
        setBottom(buildStatusBar());

        loadTasks();
        tasks.addListener((ListChangeListener<Task>) change -> onTasksChanged());
        applyFilter();
        refreshStats();

        if (prefs.getBoolean(PREF_DARK, false)) {
            getStyleClass().add("dark");
        }
    }


    private Node buildToolbar() {
        Button addButton = new Button("+ Tambah Tugas");
        addButton.getStyleClass().add("primary");
        addButton.setOnAction(e -> addTask());

        Button editButton = new Button("Edit");
        editButton.setOnAction(e -> editSelected());
        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Button deleteButton = new Button("Hapus");
        deleteButton.setOnAction(e -> deleteSelected());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        searchField.setPromptText("Cari tugas...  (Ctrl+F)");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());

        statusFilter.getItems().add(ALL_STATUS);
        for (Status s : Status.values()) {
            statusFilter.getItems().add(s.getLabel());
        }
        statusFilter.setValue(ALL_STATUS);
        statusFilter.valueProperty().addListener((obs, old, val) -> applyFilter());

        priorityFilter.getItems().add(ALL_PRIORITY);
        for (Priority p : Priority.values()) {
            priorityFilter.getItems().add(p.getLabel());
        }
        priorityFilter.setValue(ALL_PRIORITY);
        priorityFilter.valueProperty().addListener((obs, old, val) -> applyFilter());

        Button clearDone = new Button("Bersihkan yang selesai");
        clearDone.setOnAction(e -> clearCompleted());

        ToggleButton darkToggle = new ToggleButton("Mode gelap");
        darkToggle.setSelected(prefs.getBoolean(PREF_DARK, false));
        darkToggle.selectedProperty().addListener((obs, old, dark) -> setDark(dark));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox bar = new HBox(8, addButton, editButton, deleteButton,
                searchField, statusFilter, priorityFilter, spacer, clearDone, darkToggle);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    @SuppressWarnings("unchecked")
    private Node buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Belum ada tugas yang cocok.\nKlik \"+ Tambah Tugas\" untuk membuat yang baru."));

        // Kolom checkbox "selesai"
        TableColumn<Task, Boolean> doneCol = new TableColumn<>("");
        doneCol.setCellValueFactory(c -> c.getValue().statusProperty().isEqualTo(Status.DONE));
        doneCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox box = new CheckBox();

            {
                box.setOnAction(e -> {
                    Task task = rowItem(this);
                    if (task != null) {
                        task.setStatus(box.isSelected() ? Status.DONE : Status.TODO);
                    }
                });
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Boolean done, boolean empty) {
                super.updateItem(done, empty);
                if (empty || done == null) {
                    setGraphic(null);
                } else {
                    box.setSelected(done);
                    setGraphic(box);
                }
            }
        });
        doneCol.setSortable(false);
        doneCol.setResizable(false);
        doneCol.setMinWidth(46);
        doneCol.setMaxWidth(46);

        TableColumn<Task, String> titleCol = new TableColumn<>("Judul");
        titleCol.setCellValueFactory(c -> c.getValue().titleProperty());
        titleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                Task task = rowItem(this);
                setText(empty ? null : text);
                if (!empty && task != null && !task.getDescription().isBlank()) {
                    setTooltip(new Tooltip(task.getDescription()));
                } else {
                    setTooltip(null);
                }
            }
        });
        titleCol.setPrefWidth(420);

        TableColumn<Task, Priority> priorityCol = new TableColumn<>("Prioritas");
        priorityCol.setCellValueFactory(c -> c.getValue().priorityProperty());
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Priority p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().removeAll("prio-low", "prio-medium", "prio-high");
                if (empty || p == null) {
                    setText(null);
                } else {
                    setText(p.getLabel());
                    getStyleClass().add("prio-" + p.name().toLowerCase());
                }
            }
        });
        priorityCol.setPrefWidth(110);

        TableColumn<Task, Status> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> c.getValue().statusProperty());
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Status s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.getLabel());
            }
        });
        statusCol.setPrefWidth(160);

        TableColumn<Task, LocalDate> dueCol = new TableColumn<>("Tenggat");
        dueCol.setCellValueFactory(c -> c.getValue().dueDateProperty());
        dueCol.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
        dueCol.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("due-cell");
            }

            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty ? null : (date == null ? "-" : DATE_FORMAT.format(date)));
            }
        });
        dueCol.setPrefWidth(130);

        table.getColumns().addAll(doneCol, titleCol, priorityCol, statusCol, dueCol);

        table.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>() {
                @Override
                protected void updateItem(Task task, boolean empty) {
                    super.updateItem(task, empty);
                    getStyleClass().removeAll("overdue", "done-row");
                    if (!empty && task != null) {
                        if (task.getStatus() == Status.DONE) {
                            getStyleClass().add("done-row");
                        } else if (task.isOverdue()) {
                            getStyleClass().add("overdue");
                        }
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    editTask(row.getItem());
                }
            });
            return row;
        });

        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                deleteSelected();
            }
        });

        MenuItem edit = new MenuItem("Edit...");
        edit.setOnAction(e -> editSelected());
        MenuItem start = new MenuItem("Tandai sedang dikerjakan");
        start.setOnAction(e -> setSelectedStatus(Status.IN_PROGRESS));
        MenuItem done = new MenuItem("Tandai selesai");
        done.setOnAction(e -> setSelectedStatus(Status.DONE));
        MenuItem delete = new MenuItem("Hapus");
        delete.setOnAction(e -> deleteSelected());
        ContextMenu menu = new ContextMenu(edit, new SeparatorMenuItem(), start, done, new SeparatorMenuItem(), delete);
        menu.getItems().forEach(item -> item.disableProperty()
                .bind(table.getSelectionModel().selectedItemProperty().isNull()));
        table.setContextMenu(menu);

        return table;
    }

    private Node buildStatusBar() {
        progressBar.setPrefWidth(160);
        statsLabel.getStyleClass().add("stats");
        shownLabel.getStyleClass().add("muted");
        saveLabel.getStyleClass().add("muted");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox bar = new HBox(14, statsLabel, shownLabel, spacer, progressLabel, progressBar, saveLabel);
        bar.getStyleClass().add("statusbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        return bar;
    }

    private static Task rowItem(TableCell<Task, ?> cell) {
        TableRow<?> row = cell.getTableRow();
        return row == null ? null : (Task) row.getItem();
    }


    private void addTask() {
        TaskDialog.show(stage, null).ifPresent(task -> {
            tasks.add(task);
            if (filtered.contains(task)) {
                table.getSelectionModel().select(task);
                table.scrollTo(task);
            }
        });
    }

    private void editSelected() {
        Task task = table.getSelectionModel().getSelectedItem();
        if (task != null) {
            editTask(task);
        }
    }

    private void editTask(Task task) {
        TaskDialog.show(stage, task);
    }

    private void deleteSelected() {
        Task task = table.getSelectionModel().getSelectedItem();
        if (task == null) {
            return;
        }
        if (DialogUtil.confirm(stage, "Hapus tugas?",
                "Tugas \"" + task.getTitle() + "\" akan dihapus permanen.", "Ya, hapus")) {
            tasks.remove(task);
        }
    }

    private void setSelectedStatus(Status status) {
        Task task = table.getSelectionModel().getSelectedItem();
        if (task != null) {
            task.setStatus(status);
        }
    }

    private void clearCompleted() {
        long count = tasks.stream().filter(t -> t.getStatus() == Status.DONE).count();
        if (count == 0) {
            DialogUtil.error(stage, "Tidak ada yang dibersihkan", "Belum ada tugas yang berstatus selesai.");
            return;
        }
        if (DialogUtil.confirm(stage, "Bersihkan tugas selesai?",
                count + " tugas berstatus selesai akan dihapus permanen.", "Ya, bersihkan")) {
            tasks.removeIf(t -> t.getStatus() == Status.DONE);
        }
    }

    private void setDark(boolean dark) {
        getStyleClass().remove("dark");
        if (dark) {
            getStyleClass().add("dark");
        }
        prefs.putBoolean(PREF_DARK, dark);
    }

    public void installShortcuts(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::addTask);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN), this::editSelected);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), searchField::requestFocus);
    }


    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();
        String priority = priorityFilter.getValue();

        filtered.setPredicate(task -> {
            boolean matchText = query.isEmpty()
                    || task.getTitle().toLowerCase().contains(query)
                    || task.getDescription().toLowerCase().contains(query);
            boolean matchStatus = ALL_STATUS.equals(status) || task.getStatus().getLabel().equals(status);
            boolean matchPriority = ALL_PRIORITY.equals(priority) || task.getPriority().getLabel().equals(priority);
            return matchText && matchStatus && matchPriority;
        });
        refreshStats();
    }

    private void refreshStats() {
        int total = tasks.size();
        long done = tasks.stream().filter(t -> t.getStatus() == Status.DONE).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == Status.IN_PROGRESS).count();
        long overdue = tasks.stream().filter(Task::isOverdue).count();

        statsLabel.setText(String.format("Total: %d   |   Dikerjakan: %d   |   Selesai: %d   |   Terlambat: %d",
                total, inProgress, done, overdue));
        shownLabel.setText("Ditampilkan " + filtered.size() + " dari " + total);

        double ratio = total == 0 ? 0 : (double) done / total;
        progressBar.setProgress(ratio);
        progressLabel.setText(Math.round(ratio * 100) + "% selesai");
    }

    private void onTasksChanged() {
        table.refresh();
        refreshStats();
        saveLabel.setText("Menyimpan...");
        saveDelay.playFromStart();
    }

    private void loadTasks() {
        boolean firstRun = !repository.exists();
        try {
            List<Task> loaded = repository.load();
            tasks.setAll(loaded);
        } catch (IOException e) {
            Platform.runLater(() -> DialogUtil.error(stage, "Gagal memuat data",
                    "Data lama tidak dapat dibaca:\n" + e.getMessage()));
        }
        if (firstRun) {
            addSampleTasks();
        }
    }

    private void addSampleTasks() {
        LocalDate today = LocalDate.now();
        tasks.add(new Task(null, "Selamat datang di TaskFlow!",
                "Klik dua kali sebuah baris untuk mengedit. Centang kotak untuk menandai selesai.",
                Priority.LOW, Status.TODO, null, today));
        tasks.add(new Task(null, "Coba tambahkan tugas pertamamu",
                "Tekan Ctrl+N atau tombol \"+ Tambah Tugas\".",
                Priority.MEDIUM, Status.IN_PROGRESS, today.plusDays(2), today));
        tasks.add(new Task(null, "Contoh tugas yang terlambat",
                "Tenggatnya sudah lewat, jadi tampil dengan warna merah.",
                Priority.HIGH, Status.TODO, today.minusDays(3), today));
    }

    public void saveNow() {
        saveDelay.stop();
        try {
            repository.save(tasks);
            saveLabel.setText("Tersimpan otomatis pukul " + TIME_FORMAT.format(LocalTime.now()));
        } catch (IOException e) {
            saveLabel.setText("Gagal menyimpan!");
            DialogUtil.error(stage, "Gagal menyimpan", e.getMessage());
        }
    }
}
