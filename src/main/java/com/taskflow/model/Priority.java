package com.taskflow.model;

public enum Priority {
    LOW("Rendah"),
    MEDIUM("Sedang"),
    HIGH("Tinggi");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
