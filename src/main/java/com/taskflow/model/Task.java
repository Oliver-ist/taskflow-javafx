package com.taskflow.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.util.UUID;

public class Task {

    private final String id;
    private final LocalDate createdAt;

    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final ObjectProperty<Priority> priority = new SimpleObjectProperty<>(Priority.MEDIUM);
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.TODO);
    private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();

    public Task(String title) {
        this(null, title, "", Priority.MEDIUM, Status.TODO, null, null);
    }

    public Task(String id, String title, String description, Priority priority,
                Status status, LocalDate dueDate, LocalDate createdAt) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.createdAt = createdAt != null ? createdAt : LocalDate.now();
        setTitle(title);
        setDescription(description);
        setPriority(priority);
        setStatus(status);
        setDueDate(dueDate);
    }

    public boolean isOverdue() {
        LocalDate due = dueDate.get();
        return status.get() != Status.DONE && due != null && due.isBefore(LocalDate.now());
    }

    public String getId() { return id; }
    public LocalDate getCreatedAt() { return createdAt; }

    public StringProperty titleProperty() { return title; }
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value == null ? "" : value); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value == null ? "" : value); }

    public ObjectProperty<Priority> priorityProperty() { return priority; }
    public Priority getPriority() { return priority.get(); }
    public void setPriority(Priority value) { priority.set(value == null ? Priority.MEDIUM : value); }

    public ObjectProperty<Status> statusProperty() { return status; }
    public Status getStatus() { return status.get(); }
    public void setStatus(Status value) { status.set(value == null ? Status.TODO : value); }

    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }
    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate value) { dueDate.set(value); }

    @Override
    public String toString() {
        return "Task{" + getTitle() + ", " + getStatus() + ", " + getPriority() + "}";
    }
}
