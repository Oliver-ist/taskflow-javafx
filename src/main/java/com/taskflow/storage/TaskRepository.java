package com.taskflow.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.taskflow.model.Priority;
import com.taskflow.model.Status;
import com.taskflow.model.Task;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TaskRepository {

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public TaskRepository(Path file) {
        this.file = file.toAbsolutePath();
    }

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".taskflow", "tasks.json");
    }

    public Path getFile() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public List<Task> load() throws IOException {
        List<Task> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            TaskData[] data = gson.fromJson(reader, TaskData[].class);
            if (data != null) {
                for (TaskData d : data) {
                    result.add(d.toTask());
                }
            }
        } catch (JsonParseException e) {
            throw new IOException("File data tidak valid: " + file, e);
        }
        return result;
    }

    public void save(Collection<Task> tasks) throws IOException {
        Files.createDirectories(file.getParent());
        TaskData[] data = tasks.stream().map(TaskData::from).toArray(TaskData[]::new);

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static class TaskData {
        String id;
        String title;
        String description;
        String priority;
        String status;
        String dueDate;
        String createdAt;

        static TaskData from(Task t) {
            TaskData d = new TaskData();
            d.id = t.getId();
            d.title = t.getTitle();
            d.description = t.getDescription();
            d.priority = t.getPriority().name();
            d.status = t.getStatus().name();
            d.dueDate = t.getDueDate() == null ? null : t.getDueDate().toString();
            d.createdAt = t.getCreatedAt().toString();
            return d;
        }

        Task toTask() {
            return new Task(
                    id,
                    title,
                    description,
                    parseEnum(Priority.class, priority, Priority.MEDIUM),
                    parseEnum(Status.class, status, Status.TODO),
                    parseDate(dueDate),
                    parseDate(createdAt));
        }

        private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
            if (value == null) {
                return fallback;
            }
            try {
                return Enum.valueOf(type, value);
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }

        private static LocalDate parseDate(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}
