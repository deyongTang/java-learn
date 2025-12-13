package com.example.javalearn.domain.model;

import java.util.Objects;

/**
 * Value object for student identifier.
 */
public final class StudentId {
    private final String value;

    public StudentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("studentId cannot be blank");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
