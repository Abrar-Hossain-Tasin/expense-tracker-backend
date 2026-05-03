package com.poshhouse.backend.controller;

import com.poshhouse.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{reportType}")
    public ResponseEntity<byte[]> exportReport(
        @PathVariable String reportType,
        @RequestParam(required = false) String month
    ) {
        byte[] pdfBytes = reportService.generateReport(reportType, month);
        String filename = "poshhouse-" + reportType + "-" + (month == null || month.isBlank() ? "current" : month) + ".pdf";

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentLength(pdfBytes.length)
            .body(pdfBytes);
    }
}
