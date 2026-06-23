package ru.georgdeveloper.assistantcore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.georgdeveloper.assistantcore.model.DiagnosticsReport;
import ru.georgdeveloper.assistantcore.repository.DiagnosticsReportRepository;
import ru.georgdeveloper.assistantcore.service.DiagnosticsFileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private DiagnosticsReportRepository diagnosticsReportRepository;

    @Autowired
    private DiagnosticsFileStorageService fileStorageService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createReport(
            @RequestParam("detection_date") String detectionDate,
            @RequestParam("diagnostics_type") String diagnosticsType,
            @RequestParam("area") String area,
            @RequestParam("equipment") String equipment,
            @RequestParam(value = "node", required = false) String node,
            @RequestParam(value = "malfunction", required = false) String malfunction,
            @RequestParam(value = "additional_kit", required = false) String additionalKit,
            @RequestParam(value = "causes", required = false) String causes,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "document", required = false) MultipartFile document) {

        try {
            DiagnosticsReport report = new DiagnosticsReport();
            report.setDetectionDate(LocalDate.parse(detectionDate, DATE_FORMATTER));
            report.setDiagnosticsType(diagnosticsType);
            report.setArea(area);
            report.setEquipment(equipment);
            report.setNode(node);
            report.setMalfunction(malfunction);
            report.setAdditionalKit(additionalKit);
            report.setCauses(causes);
            report.setStatus("ОТКРЫТО");

            if (photo != null && !photo.isEmpty()) {
                report.setPhotoPath(fileStorageService.saveFile(photo));
            }

            if (document != null && !document.isEmpty()) {
                report.setDocumentPath(fileStorageService.saveFile(document));
            }

            DiagnosticsReport savedReport = diagnosticsReportRepository.save(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", savedReport.getId());
            response.put("message", "Отчет успешно создан");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to save diagnostics files", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при сохранении файлов: " + describeIoError(e));
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            log.error("Failed to create diagnostics report", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при создании отчета: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateReport(
            @PathVariable Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "elimination_date", required = false) String eliminationDate,
            @RequestParam(value = "condition_after_elimination", required = false) String conditionAfterElimination,
            @RequestParam(value = "responsible", required = false) String responsible,
            @RequestParam(value = "non_elimination_reason", required = false) String nonEliminationReason,
            @RequestParam(value = "measures", required = false) String measures,
            @RequestParam(value = "comments", required = false) String comments,
            @RequestParam(value = "photo_result", required = false) MultipartFile photoResult,
            @RequestParam(value = "document_result", required = false) MultipartFile documentResult) {

        try {
            DiagnosticsReport report = diagnosticsReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Отчет не найден"));

            report.setStatus(status);

            if (responsible != null && !responsible.isEmpty()) {
                report.setResponsible(responsible);
            }

            if ("ЗАКРЫТО".equals(status)) {
                if (eliminationDate != null && !eliminationDate.isEmpty()) {
                    report.setEliminationDate(LocalDate.parse(eliminationDate, DATE_FORMATTER));
                }
                if (conditionAfterElimination != null) {
                    report.setConditionAfterElimination(conditionAfterElimination);
                }
                if (nonEliminationReason != null) {
                    report.setNonEliminationReason(nonEliminationReason);
                }
                if (measures != null) {
                    report.setMeasures(measures);
                }
                if (comments != null) {
                    report.setComments(comments);
                }
            }

            if (photoResult != null && !photoResult.isEmpty()) {
                report.setPhotoResultPath(fileStorageService.saveFile(photoResult));
            }

            if (documentResult != null && !documentResult.isEmpty()) {
                report.setDocumentResultPath(fileStorageService.saveFile(documentResult));
            }

            diagnosticsReportRepository.save(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Отчет успешно обновлен");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating diagnostics report {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении отчета: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<Map<String, Object>> updateReportData(
            @PathVariable Long id,
            @RequestParam(value = "detection_date", required = false) String detectionDate,
            @RequestParam(value = "diagnostics_type", required = false) String diagnosticsType,
            @RequestParam(value = "equipment", required = false) String equipment,
            @RequestParam(value = "node", required = false) String node,
            @RequestParam(value = "area", required = false) String area,
            @RequestParam(value = "malfunction", required = false) String malfunction,
            @RequestParam(value = "additional_kit", required = false) String additionalKit,
            @RequestParam(value = "causes", required = false) String causes,
            @RequestParam(value = "report", required = false) String report,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "elimination_date", required = false) String eliminationDate,
            @RequestParam(value = "condition_after_elimination", required = false) String conditionAfterElimination,
            @RequestParam(value = "responsible", required = false) String responsible,
            @RequestParam(value = "non_elimination_reason", required = false) String nonEliminationReason,
            @RequestParam(value = "measures", required = false) String measures,
            @RequestParam(value = "comments", required = false) String comments,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "document", required = false) MultipartFile document,
            @RequestParam(value = "photo_result", required = false) MultipartFile photoResult,
            @RequestParam(value = "document_result", required = false) MultipartFile documentResult) {

        try {
            DiagnosticsReport reportEntity = diagnosticsReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Отчет не найден"));

            if (detectionDate != null && !detectionDate.isEmpty()) {
                reportEntity.setDetectionDate(LocalDate.parse(detectionDate, DATE_FORMATTER));
            }
            if (diagnosticsType != null) {
                reportEntity.setDiagnosticsType(diagnosticsType);
            }
            if (equipment != null) {
                reportEntity.setEquipment(equipment);
            }
            if (node != null) {
                reportEntity.setNode(node);
            }
            if (area != null) {
                reportEntity.setArea(area);
            }
            if (malfunction != null) {
                reportEntity.setMalfunction(malfunction);
            }
            if (additionalKit != null) {
                reportEntity.setAdditionalKit(additionalKit);
            }
            if (causes != null) {
                reportEntity.setCauses(causes);
            }
            if (report != null) {
                reportEntity.setReport(report);
            }
            if (status != null) {
                reportEntity.setStatus(status);
            }
            if (eliminationDate != null && !eliminationDate.isEmpty()) {
                reportEntity.setEliminationDate(LocalDate.parse(eliminationDate, DATE_FORMATTER));
            }
            if (conditionAfterElimination != null) {
                reportEntity.setConditionAfterElimination(conditionAfterElimination);
            }
            if (responsible != null) {
                reportEntity.setResponsible(responsible);
            }
            if (nonEliminationReason != null) {
                reportEntity.setNonEliminationReason(nonEliminationReason);
            }
            if (measures != null) {
                reportEntity.setMeasures(measures);
            }
            if (comments != null) {
                reportEntity.setComments(comments);
            }

            if (photo != null && !photo.isEmpty()) {
                deleteStoredFileQuietly(reportEntity.getPhotoPath());
                reportEntity.setPhotoPath(fileStorageService.saveFile(photo));
            }

            if (document != null && !document.isEmpty()) {
                deleteStoredFileQuietly(reportEntity.getDocumentPath());
                reportEntity.setDocumentPath(fileStorageService.saveFile(document));
            }

            if (photoResult != null && !photoResult.isEmpty()) {
                reportEntity.setPhotoResultPath(fileStorageService.saveFile(photoResult));
            }

            if (documentResult != null && !documentResult.isEmpty()) {
                reportEntity.setDocumentResultPath(fileStorageService.saveFile(documentResult));
            }

            diagnosticsReportRepository.save(reportEntity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Отчет успешно обновлен");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating diagnostics report {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении отчета: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long id) {
        try {
            DiagnosticsReport report = diagnosticsReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Отчет не найден"));

            deleteStoredFileQuietly(report.getPhotoPath());
            deleteStoredFileQuietly(report.getDocumentPath());
            deleteStoredFileQuietly(report.getPhotoResultPath());
            deleteStoredFileQuietly(report.getDocumentResultPath());

            diagnosticsReportRepository.delete(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Отчет успешно удален");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting diagnostics report {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при удалении отчета: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/{id}/file")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long id,
            @RequestParam("file_type") String fileType) {

        try {
            DiagnosticsReport report = diagnosticsReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Отчет не найден"));

            String filePath = null;

            if ("photo".equals(fileType)) {
                filePath = report.getPhotoPath();
                report.setPhotoPath(null);
            } else if ("document".equals(fileType)) {
                filePath = report.getDocumentPath();
                report.setDocumentPath(null);
            } else if ("photo_result".equals(fileType)) {
                filePath = report.getPhotoResultPath();
                report.setPhotoResultPath(null);
            } else if ("document_result".equals(fileType)) {
                filePath = report.getDocumentResultPath();
                report.setDocumentResultPath(null);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Неизвестный тип файла");
                return ResponseEntity.status(400).body(response);
            }

            deleteStoredFileQuietly(filePath);
            diagnosticsReportRepository.save(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Файл успешно удален");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting file from report {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при удалении файла: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/files/**")
    public ResponseEntity<Resource> getFile(@RequestParam(value = "path", required = false) String pathParam,
                                            org.springframework.web.context.request.ServletWebRequest request) {
        try {
            String filePath = pathParam;
            if (filePath == null || filePath.isEmpty()) {
                String requestURI = request.getRequest().getRequestURI();
                int filesIndex = requestURI.indexOf("/files/");
                if (filesIndex == -1) {
                    return ResponseEntity.notFound().build();
                }
                filePath = requestURI.substring(filesIndex + 7);
            }

            String decodedFilePath = decodePath(filePath);
            if (decodedFilePath.startsWith("/")) {
                decodedFilePath = decodedFilePath.substring(1);
            }

            Path file = fileStorageService.resolveStoredFile(decodedFilePath);
            if (file == null) {
                log.warn("Diagnostics file not found: {}", decodedFilePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String disposition = request.getParameter("download") != null && "true".equals(request.getParameter("download"))
                    ? "attachment" : "inline";

            String fileName = file.getFileName().toString();
            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            boolean hasNonAscii = fileName.chars().anyMatch(ch -> ch > 127);
            String contentDisposition = hasNonAscii
                    ? disposition + "; filename*=UTF-8''" + encodedFileName
                    : disposition + "; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to read diagnostics file", e);
            return ResponseEntity.notFound().build();
        }
    }

    private void deleteStoredFileQuietly(String storedPath) {
        if (storedPath == null) {
            return;
        }
        try {
            fileStorageService.deleteStoredFile(storedPath);
        } catch (IOException e) {
            log.warn("Failed to delete diagnostics file {}: {}", storedPath, e.getMessage());
        }
    }

    private String decodePath(String filePath) {
        try {
            String decoded = java.net.URLDecoder.decode(filePath, java.nio.charset.StandardCharsets.UTF_8);
            if (decoded.contains("%")) {
                decoded = java.net.URLDecoder.decode(decoded, java.nio.charset.StandardCharsets.UTF_8);
            }
            return decoded;
        } catch (Exception e) {
            return filePath;
        }
    }

    private String describeIoError(IOException e) {
        String details = e.getClass().getSimpleName();
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            details += ": " + e.getMessage();
        }
        details += " (каталог: " + fileStorageService.getUploadDir() + ")";
        return details;
    }
}
