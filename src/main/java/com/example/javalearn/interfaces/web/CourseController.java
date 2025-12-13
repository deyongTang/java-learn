package com.example.javalearn.interfaces.web;

import com.example.javalearn.application.CourseAppService;
import com.example.javalearn.application.command.EnrollCommand;
import com.example.javalearn.application.view.CourseView;
import com.example.javalearn.domain.event.CourseEnrolled;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseAppService courseAppService;

    public CourseController(CourseAppService courseAppService) {
        this.courseAppService = courseAppService;
    }

    @GetMapping("/{courseId}")
    public CourseView getCourse(@PathVariable String courseId) {
        return courseAppService.getCourse(courseId);
    }

    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<CourseEnrolled> enroll(@PathVariable String courseId, @RequestBody EnrollRequest request) {
        CourseEnrolled event = courseAppService.enroll(new EnrollCommand(courseId, request.studentId()));
        return ResponseEntity.ok(event);
    }

    public record EnrollRequest(String studentId) {
    }
}
