package com.eams.controller;

import com.eams.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> summary() {
        return ResponseEntity.ok(reportService.getSummaryReport());
    }

    @GetMapping("/category-wise")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> categoryWise() {
        return ResponseEntity.ok(reportService.getCategoryWiseReport());
    }

    @GetMapping("/student-wise")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> studentWise() {
        return ResponseEntity.ok(reportService.getStudentWiseReport());
    }
}
