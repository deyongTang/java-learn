package com.example.javalearn.domain.repository;

import com.example.javalearn.domain.model.Course;
import com.example.javalearn.domain.model.CourseId;

import java.util.Optional;

public interface CourseRepository {
    Optional<Course> find(CourseId id);

    void save(Course course);
}
