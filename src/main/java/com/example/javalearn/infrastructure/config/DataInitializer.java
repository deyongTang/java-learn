package com.example.javalearn.infrastructure.config;

import com.example.javalearn.domain.model.Course;
import com.example.javalearn.domain.model.CourseId;
import com.example.javalearn.infrastructure.persistence.InMemoryCourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedCourses(InMemoryCourseRepository repository) {
        return args -> {
            repository.put(new Course(new CourseId("math-101"), "Math 101", 2));
            repository.put(new Course(new CourseId("cs-101"), "Intro to CS", 3));
        };
    }
}
