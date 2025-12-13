package com.example.javalearn.domain.model;

import com.example.javalearn.domain.event.CourseEnrolled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Aggregate root: Course manages enrollment invariants (capacity, uniqueness).
 */
public class Course {
    private final CourseId id;
    private final String name;
    private final int capacity;
    private final Set<StudentId> enrolled;
    private final List<CourseEnrolled> pendingEvents = new ArrayList<>();

    public Course(CourseId id, String name, int capacity, Set<StudentId> enrolled) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.enrolled = new LinkedHashSet<>(enrolled);
    }

    public Course(CourseId id, String name, int capacity) {
        this(id, name, capacity, Set.of());
    }

    public CourseEnrolled enroll(StudentId studentId) {
        if (enrolled.contains(studentId)) {
            throw new IllegalStateException("student already enrolled");
        }
        if (enrolled.size() >= capacity) {
            throw new IllegalStateException("course is full");
        }
        enrolled.add(studentId);
        CourseEnrolled event = new CourseEnrolled(id, studentId);
        pendingEvents.add(event);
        return event;
    }

    public CourseId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public Set<StudentId> getEnrolled() {
        return Collections.unmodifiableSet(enrolled);
    }

    public List<CourseEnrolled> getPendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearPendingEvents() {
        pendingEvents.clear();
    }
}
