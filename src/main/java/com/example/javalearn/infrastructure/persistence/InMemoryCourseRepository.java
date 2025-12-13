package com.example.javalearn.infrastructure.persistence;

import com.example.javalearn.domain.model.Course;
import com.example.javalearn.domain.model.CourseId;
import com.example.javalearn.domain.repository.CourseRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCourseRepository implements CourseRepository {

    private final Map<String, Course> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<Course> find(CourseId id) {
        return Optional.ofNullable(storage.get(id.getValue()));
    }

    @Override
    public void save(Course course) {
        storage.put(course.getId().getValue(), course);
    }

    public void put(Course course) {
        save(course);
    }
}
