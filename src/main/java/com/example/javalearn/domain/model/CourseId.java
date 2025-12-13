package com.example.javalearn.domain.model;

import java.util.Objects;

/**
 * Value object for course identifier.
 */
public final class CourseId {
    private final String value;

    public CourseId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("courseId cannot be blank");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseId courseId)) return false;
        return value.equals(courseId.value);
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
