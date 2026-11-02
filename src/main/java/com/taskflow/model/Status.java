package com.taskflow.model;

public enum Status {
    TODO("Belum dikerjakan"),
    IN_PROGRESS("Sedang dikerjakan"),
    DONE("Selesai");

    private final String label;

    Status(String label) {
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
