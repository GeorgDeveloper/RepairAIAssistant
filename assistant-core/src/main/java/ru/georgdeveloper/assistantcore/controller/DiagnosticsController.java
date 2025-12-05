package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.georgdeveloper.assistantcore.model.DiagnosticsReport;
import ru.georgdeveloper.assistantcore.repository.DiagnosticsReportRepository;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    @Autowired
    private DiagnosticsReportRepository diagnosticsReportRepository;

    private static final String UPLOAD_DIR = "./uploads/diagnostics/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
            // Создаем директорию для загрузок, если её нет
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            DiagnosticsReport report = new DiagnosticsReport();
            report.setDetectionDate(LocalDate.parse(detectionDate, DATE_FORMATTER));
            report.setDiagnosticsType(diagnosticsType);
            report.setArea(area);
            report.setEquipment(equipment);
            report.setNode(node);
            report.setMalfunction(malfunction);
            report.setAdditionalKit(additionalKit);
            report.setCauses(causes);
            report.setStatus("ОТКРЫТО"); // Статус по умолчанию

            // Сохраняем фото, если загружено
            if (photo != null && !photo.isEmpty()) {
                String originalFileName = photo.getOriginalFilename();
                // Сохраняем оригинальное имя файла с пробелами и спецсимволами
                String photoFileName = UUID.randomUUID().toString() + "_" + originalFileName;
                Path photoPath = uploadPath.resolve(photoFileName);
                Files.copy(photo.getInputStream(), photoPath);
                // Сохраняем путь с правильным форматом
                report.setPhotoPath("/uploads/diagnostics/" + photoFileName);
                System.out.println("Photo saved: " + photoPath.toAbsolutePath());
            }

            // Сохраняем документ, если загружен
            if (document != null && !document.isEmpty()) {
                String originalFileName = document.getOriginalFilename();
                // Сохраняем оригинальное имя файла с пробелами и спецсимволами
                String docFileName = UUID.randomUUID().toString() + "_" + originalFileName;
                Path docPath = uploadPath.resolve(docFileName);
                Files.copy(document.getInputStream(), docPath);
                // Сохраняем путь с правильным форматом
                report.setDocumentPath("/uploads/diagnostics/" + docFileName);
                System.out.println("Document saved: " + docPath.toAbsolutePath());
            }

            DiagnosticsReport savedReport = diagnosticsReportRepository.save(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", savedReport.getId());
            response.put("message", "Отчет успешно создан");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при сохранении файлов: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
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

            // Устанавливаем ответственного для статуса "В РАБОТЕ" или "ЗАКРЫТО"
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

            // Сохраняем итоговые файлы
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (photoResult != null && !photoResult.isEmpty()) {
                String photoFileName = UUID.randomUUID().toString() + "_" + photoResult.getOriginalFilename();
                Path photoPath = uploadPath.resolve(photoFileName);
                Files.copy(photoResult.getInputStream(), photoPath);
                report.setPhotoResultPath("/uploads/diagnostics/" + photoFileName);
            }

            if (documentResult != null && !documentResult.isEmpty()) {
                String docFileName = UUID.randomUUID().toString() + "_" + documentResult.getOriginalFilename();
                Path docPath = uploadPath.resolve(docFileName);
                Files.copy(documentResult.getInputStream(), docPath);
                report.setDocumentResultPath("/uploads/diagnostics/" + docFileName);
            }

            diagnosticsReportRepository.save(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Отчет успешно обновлен");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error updating diagnostics report " + id + ": " + e.getMessage());
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

            // Сохраняем новые файлы, если загружены
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (photo != null && !photo.isEmpty()) {
                // Удаляем старое фото, если есть
                if (reportEntity.getPhotoPath() != null) {
                    try {
                        String oldPhotoPath = reportEntity.getPhotoPath();
                        if (oldPhotoPath.startsWith("/uploads/diagnostics/")) {
                            oldPhotoPath = oldPhotoPath.substring("/uploads/diagnostics/".length());
                        }
                        Path oldPhoto = uploadPath.resolve(oldPhotoPath);
                        if (Files.exists(oldPhoto)) {
                            Files.delete(oldPhoto);
                        }
                    } catch (Exception e) {
                        System.err.println("Error deleting old photo: " + e.getMessage());
                    }
                }
                // Сохраняем новое фото
                String photoFileName = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
                Path photoPath = uploadPath.resolve(photoFileName);
                Files.copy(photo.getInputStream(), photoPath);
                reportEntity.setPhotoPath("/uploads/diagnostics/" + photoFileName);
            }

            if (document != null && !document.isEmpty()) {
                // Удаляем старый документ, если есть
                if (reportEntity.getDocumentPath() != null) {
                    try {
                        String oldDocPath = reportEntity.getDocumentPath();
                        if (oldDocPath.startsWith("/uploads/diagnostics/")) {
                            oldDocPath = oldDocPath.substring("/uploads/diagnostics/".length());
                        }
                        Path oldDoc = uploadPath.resolve(oldDocPath);
                        if (Files.exists(oldDoc)) {
                            Files.delete(oldDoc);
                        }
                    } catch (Exception e) {
                        System.err.println("Error deleting old document: " + e.getMessage());
                    }
                }
                // Сохраняем новый документ
                String docFileName = UUID.randomUUID().toString() + "_" + document.getOriginalFilename();
                Path docPath = uploadPath.resolve(docFileName);
                Files.copy(document.getInputStream(), docPath);
                reportEntity.setDocumentPath("/uploads/diagnostics/" + docFileName);
            }

            diagnosticsReportRepository.save(reportEntity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Отчет успешно обновлен");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error updating diagnostics report " + id + ": " + e.getMessage());
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

            // Удаляем файлы, если есть
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (report.getPhotoPath() != null) {
                try {
                    String photoPath = report.getPhotoPath();
                    if (photoPath.startsWith("/uploads/diagnostics/")) {
                        photoPath = photoPath.substring("/uploads/diagnostics/".length());
                    }
                    Path photoFile = uploadPath.resolve(photoPath);
                    if (Files.exists(photoFile)) {
                        Files.delete(photoFile);
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting photo: " + e.getMessage());
                }
            }

            if (report.getDocumentPath() != null) {
                try {
                    String docPath = report.getDocumentPath();
                    if (docPath.startsWith("/uploads/diagnostics/")) {
                        docPath = docPath.substring("/uploads/diagnostics/".length());
                    }
                    Path docFile = uploadPath.resolve(docPath);
                    if (Files.exists(docFile)) {
                        Files.delete(docFile);
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting document: " + e.getMessage());
                }
            }

            if (report.getPhotoResultPath() != null) {
                try {
                    String photoPath = report.getPhotoResultPath();
                    if (photoPath.startsWith("/uploads/diagnostics/")) {
                        photoPath = photoPath.substring("/uploads/diagnostics/".length());
                    }
                    Path photoFile = uploadPath.resolve(photoPath);
                    if (Files.exists(photoFile)) {
                        Files.delete(photoFile);
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting result photo: " + e.getMessage());
                }
            }

            if (report.getDocumentResultPath() != null) {
                try {
                    String docPath = report.getDocumentResultPath();
                    if (docPath.startsWith("/uploads/diagnostics/")) {
                        docPath = docPath.substring("/uploads/diagnostics/".length());
                    }
                    Path docFile = uploadPath.resolve(docPath);
                    if (Files.exists(docFile)) {
                        Files.delete(docFile);
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting result document: " + e.getMessage());
                }
            }

            diagnosticsReportRepository.delete(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Отчет успешно удален");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error deleting diagnostics report " + id + ": " + e.getMessage());
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

            Path uploadPath = Paths.get(UPLOAD_DIR);
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

            // Удаляем файл с диска
            if (filePath != null) {
                try {
                    if (filePath.startsWith("/uploads/diagnostics/")) {
                        filePath = filePath.substring("/uploads/diagnostics/".length());
                    }
                    Path file = uploadPath.resolve(filePath);
                    if (Files.exists(file)) {
                        Files.delete(file);
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting file: " + e.getMessage());
                }
            }

            diagnosticsReportRepository.save(report);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Файл успешно удален");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error deleting file from report " + id + ": " + e.getMessage());
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
            String filePath;
            
            // Пытаемся получить путь из параметра или из URI
            if (pathParam != null && !pathParam.isEmpty()) {
                filePath = pathParam;
            } else {
                // Получаем путь из URL
                String requestURI = request.getRequest().getRequestURI();
                int filesIndex = requestURI.indexOf("/files/");
                if (filesIndex == -1) {
                    return ResponseEntity.notFound().build();
                }
                filePath = requestURI.substring(filesIndex + 7);
            }
            
            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Декодируем URL для правильной обработки кириллицы и пробелов
            // Может потребоваться двойное декодирование, если путь был закодирован дважды
            String decodedFilePath = filePath;
            try {
                // Первое декодирование
                decodedFilePath = java.net.URLDecoder.decode(filePath, java.nio.charset.StandardCharsets.UTF_8.toString());
                // Если после декодирования все еще есть закодированные символы, декодируем еще раз
                if (decodedFilePath.contains("%")) {
                    decodedFilePath = java.net.URLDecoder.decode(decodedFilePath, java.nio.charset.StandardCharsets.UTF_8.toString());
                }
                System.out.println("Core: Original filePath: " + filePath);
                System.out.println("Core: Decoded filePath: " + decodedFilePath);
            } catch (Exception e) {
                // Если декодирование не удалось, используем исходный путь
                System.err.println("Core: Failed to decode path: " + filePath);
            }
            
            // Убираем начальный слэш, если есть
            if (decodedFilePath.startsWith("/")) {
                decodedFilePath = decodedFilePath.substring(1);
            }
            
            // Если путь начинается с uploads/diagnostics, убираем этот префикс
            // так как файл уже находится в UPLOAD_DIR
            String actualFilePath = decodedFilePath;
            if (decodedFilePath.startsWith("uploads/diagnostics/")) {
                actualFilePath = decodedFilePath.substring("uploads/diagnostics/".length());
            } else if (decodedFilePath.startsWith("uploads/")) {
                // Если просто uploads, убираем префикс
                actualFilePath = decodedFilePath.substring("uploads/".length());
            }
            
            // Формируем полный путь к файлу
            // Пробуем несколько возможных путей, так как при запуске из IDE файлы могут быть в assistant-core/uploads/diagnostics/
            Path uploadDir = null;
            Path file = null;
            
            // Пробуем несколько возможных путей
            // 1. Стандартный путь (для запуска из JAR)
            Path standardUploadDir = Paths.get(UPLOAD_DIR).normalize().toAbsolutePath();
            Path standardFile = standardUploadDir.resolve(actualFilePath).normalize();
            
            // 2. Путь для запуска из IDE (относительно корня проекта)
            // Получаем текущую рабочую директорию
            Path currentDir = Paths.get(System.getProperty("user.dir")).normalize();
            Path ideUploadDir = currentDir.resolve("assistant-core").resolve("uploads").resolve("diagnostics").normalize();
            Path ideFile = ideUploadDir.resolve(actualFilePath).normalize();
            
            // 3. Путь относительно assistant-core модуля
            Path moduleUploadDir = Paths.get("assistant-core", "uploads", "diagnostics").normalize().toAbsolutePath();
            Path moduleFile = moduleUploadDir.resolve(actualFilePath).normalize();
            
            // Проверяем все варианты
            System.out.println("Core: Checking paths:");
            System.out.println("  1. Standard: " + standardFile.toAbsolutePath() + " (exists: " + Files.exists(standardFile) + ")");
            System.out.println("  2. IDE: " + ideFile.toAbsolutePath() + " (exists: " + Files.exists(ideFile) + ")");
            System.out.println("  3. Module: " + moduleFile.toAbsolutePath() + " (exists: " + Files.exists(moduleFile) + ")");
            
            if (Files.exists(standardFile) && Files.isRegularFile(standardFile)) {
                file = standardFile;
                uploadDir = standardUploadDir;
                System.out.println("Core: Using standard path");
            } else if (Files.exists(ideFile) && Files.isRegularFile(ideFile)) {
                file = ideFile;
                uploadDir = ideUploadDir;
                System.out.println("Core: Using IDE path");
            } else if (Files.exists(moduleFile) && Files.isRegularFile(moduleFile)) {
                file = moduleFile;
                uploadDir = moduleUploadDir;
                System.out.println("Core: Using module path");
            } else {
                // Используем стандартный путь для логирования ошибки
                file = standardFile;
                uploadDir = standardUploadDir;
                System.out.println("Core: File not found in any location");
            }
            
            // Проверяем, что файл находится в пределах UPLOAD_DIR (защита от path traversal)
            if (!file.startsWith(uploadDir)) {
                System.err.println("=== Path traversal attempt ===");
                System.err.println("Request URI: " + request.getRequest().getRequestURI());
                System.err.println("File path: " + file.toAbsolutePath());
                System.err.println("Upload dir: " + uploadDir);
                return ResponseEntity.notFound().build();
            }
            
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                // Пытаемся найти файл по части имени (UUID + часть после подчеркивания)
                try {
                    java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir);
                    String searchPattern = null;
                    if (actualFilePath.contains("_")) {
                        // Берем часть после первого подчеркивания (UUID_original_name)
                        searchPattern = actualFilePath.substring(actualFilePath.indexOf("_") + 1);
                    } else {
                        searchPattern = actualFilePath;
                    }
                    
                    for (Path entry : stream) {
                        String entryName = entry.getFileName().toString();
                        // Проверяем, заканчивается ли имя файла на искомый паттерн
                        if (entryName.contains("_") && entryName.endsWith(searchPattern)) {
                            file = entry;
                            System.out.println("Core: Found file by pattern: " + file.toAbsolutePath());
                            break;
                        }
                        // Также проверяем точное совпадение после нормализации пробелов
                        String normalizedEntryName = entryName.replaceAll("\\s+", " ").trim();
                        String normalizedSearch = actualFilePath.replaceAll("\\s+", " ").trim();
                        if (normalizedEntryName.equals(normalizedSearch)) {
                            file = entry;
                            System.out.println("Core: Found file by normalized name: " + file.toAbsolutePath());
                            break;
                        }
                    }
                    stream.close();
                } catch (Exception e) {
                    System.err.println("Cannot list directory: " + e.getMessage());
                }
                
                // Если файл все еще не найден, логируем и возвращаем 404
                if (!Files.exists(file) || !Files.isRegularFile(file)) {
                    System.err.println("=== File not found ===");
                    System.err.println("Request URI: " + request.getRequest().getRequestURI());
                    System.err.println("Original filePath: " + filePath);
                    System.err.println("Decoded filePath: " + decodedFilePath);
                    System.err.println("Actual filePath: " + actualFilePath);
                    System.err.println("Looking for: " + file.toAbsolutePath());
                    System.err.println("Upload dir: " + uploadDir);
                    return ResponseEntity.notFound().build();
                }
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // Определяем, нужно ли скачивание или просмотр
            String disposition = "inline";
            if (request.getParameter("download") != null && "true".equals(request.getParameter("download"))) {
                disposition = "attachment";
            }
            
            // Правильно кодируем имя файла для заголовка Content-Disposition (RFC 5987)
            String fileName = file.getFileName().toString();
            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // Проверяем, содержит ли имя файла не-ASCII символы
            boolean hasNonAscii = fileName.chars().anyMatch(ch -> ch > 127);
            
            // Формируем заголовок Content-Disposition
            String contentDisposition;
            if (hasNonAscii) {
                // Если есть не-ASCII символы, используем только filename* (RFC 5987)
                contentDisposition = disposition + "; filename*=UTF-8''" + encodedFileName;
            } else {
                // Если только ASCII, используем оба варианта для совместимости
                contentDisposition = disposition + "; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName;
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
                    
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

