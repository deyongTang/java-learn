package com.example.javalearn.application.view;

import java.util.Set;

public record CourseView(String id, String name, int capacity, Set<String> enrolledStudentIds) {
}
