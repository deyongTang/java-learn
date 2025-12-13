package com.example.javalearn.domain.event;

import com.example.javalearn.domain.model.CourseId;
import com.example.javalearn.domain.model.StudentId;

/**
 * Domain event representing a successful enrollment.
 */
public record CourseEnrolled(CourseId courseId, StudentId studentId) {
}
