package com.taskflow.storage;

import com.taskflow.model.Priority;
import com.taskflow.model.Status;
import com.taskflow.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsEmptyListWhenFileDoesNotExist() throws IOException {
        TaskRepository repo = new TaskRepository(tempDir.resolve("tasks.json"));
        assertFalse(repo.exists());
        assertTrue(repo.load().isEmpty());
    }

    @Test
    void saveThenLoadKeepsAllFields() throws IOException {
        TaskRepository repo = new TaskRepository(tempDir.resolve("data").resolve("tasks.json"));
        Task original = new Task("id-1", "Belajar JavaFX", "Bab TableView",
                Priority.HIGH, Status.IN_PROGRESS, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 11, 1));
        Task noDue = new Task("Tanpa tenggat");

        repo.save(List.of(original, noDue));
        List<Task> loaded = repo.load();

        assertEquals(2, loaded.size());
        Task first = loaded.get(0);
        assertEquals("id-1", first.getId());
        assertEquals("Belajar JavaFX", first.getTitle());
        assertEquals("Bab TableView", first.getDescription());
        assertEquals(Priority.HIGH, first.getPriority());
        assertEquals(Status.IN_PROGRESS, first.getStatus());
        assertEquals(LocalDate.of(2026, 12, 31), first.getDueDate());
        assertEquals(LocalDate.of(2026, 11, 1), first.getCreatedAt());
        assertNull(loaded.get(1).getDueDate());
    }

    @Test
    void unknownEnumValuesFallBackToDefaults() throws IOException {
        Path file = tempDir.resolve("tasks.json");
        Files.writeString(file, "[{\"id\":\"x\",\"title\":\"A\",\"priority\":\"BOGUS\",\"status\":\"???\"}]");

        List<Task> loaded = new TaskRepository(file).load();

        assertEquals(Priority.MEDIUM, loaded.get(0).getPriority());
        assertEquals(Status.TODO, loaded.get(0).getStatus());
    }

    @Test
    void corruptedFileThrowsIOException() throws IOException {
        Path file = tempDir.resolve("tasks.json");
        Files.writeString(file, "{ ini bukan json yang valid");

        assertThrows(IOException.class, () -> new TaskRepository(file).load());
    }
}
