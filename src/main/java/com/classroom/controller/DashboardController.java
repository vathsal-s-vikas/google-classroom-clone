package com.classroom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard/student")
    public String studentDashboard() {
        return "student"; // looks for student.html in /resources/templates
    }

    @GetMapping("/dashboard/teacher")
    public String teacherDashboard() {
        return "teacher";
    }

    @GetMapping("/dashboard/ta")
    public String taDashboard() {
        return "ta";
    }
}
