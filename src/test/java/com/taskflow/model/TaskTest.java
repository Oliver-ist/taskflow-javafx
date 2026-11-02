package com.taskflow.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    @Test
    void newTaskHasSensibleDefaults() {
        Task task = new Task("Tulis README");
        assertEquals("Tulis README", task.getTitle());
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals(Status.TODO, task.getStatus());
        assertEquals(LocalDate.now(), task.getCreatedAt());
        assertFalse(task.getId().isBlank());
    }

    @Test
    void eachTaskGetsUniqueId() {
        assertNotEquals(new Task("A").getId(), new Task("B").getId());
    }

    @Test
    void taskIsOverdueOnlyWhenNotDoneAndDueDatePassed() {
        Task task = new Task("Laporan");
        task.setDueDate(LocalDate.now().minusDays(1));
        assertTrue(task.isOverdue());

        task.setStatus(Status.DONE);
        assertFalse(task.isOverdue());

        task.setStatus(Status.TODO);
        task.setDueDate(LocalDate.now());
        assertFalse(task.isOverdue());

        task.setDueDate(null);
        assertFalse(task.isOverdue());
    }

    @Test
    void nullValuesAreReplacedWithDefaults() {
        Task task = new Task("X");
        task.setTitle(null);
        task.setPriority(null);
        task.setStatus(null);
        assertEquals("", task.getTitle());
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals(Status.TODO, task.getStatus());
    }
}
