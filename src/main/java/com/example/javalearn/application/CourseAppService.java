package com.example.javalearn.application;

import com.example.javalearn.application.command.EnrollCommand;
import com.example.javalearn.application.view.CourseView;
import com.example.javalearn.domain.event.CourseEnrolled;
import com.example.javalearn.domain.model.Course;
import com.example.javalearn.domain.model.CourseId;
import com.example.javalearn.domain.model.StudentId;
import com.example.javalearn.domain.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseAppService {

    private final CourseRepository courseRepository;

    public CourseAppService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional
    public CourseEnrolled enroll(EnrollCommand command) {
        Course course = courseRepository.find(new CourseId(command.courseId()))
                .orElseThrow(() -> new IllegalArgumentException("course not found"));
        CourseEnrolled event = course.enroll(new StudentId(command.studentId()));
        courseRepository.save(course);
        // In a real app, publish event here.
        course.clearPendingEvents();
        return event;
    }

    @Transactional(readOnly = true)
    public CourseView getCourse(String courseId) {
        Course course = courseRepository.find(new CourseId(courseId))
                .orElseThrow(() -> new IllegalArgumentException("course not found"));
        Set<String> enrolled = course.getEnrolled().stream()
                .map(StudentId::getValue)
                .collect(Collectors.toUnmodifiableSet());
        return new CourseView(course.getId().getValue(), course.getName(), course.getCapacity(), enrolled);
    }
}
