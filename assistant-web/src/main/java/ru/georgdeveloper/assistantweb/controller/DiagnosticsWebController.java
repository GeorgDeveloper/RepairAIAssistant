package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public DiagnosticsWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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
            // Проксируем запрос к core-сервису
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("detection_date", detectionDate);
            body.add("diagnostics_type", diagnosticsType);
            body.add("area", area);
            body.add("equipment", equipment);
            if (node != null) body.add("node", node);
            if (malfunction != null) body.add("malfunction", malfunction);
            if (additionalKit != null) body.add("additional_kit", additionalKit);
            if (causes != null) body.add("causes", causes);
            if (photo != null && !photo.isEmpty()) {
                body.add("photo", photo.getResource());
            }
            if (document != null && !document.isEmpty()) {
                body.add("document", document.getResource());
            }

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) 
                    restTemplate.postForEntity(
                            coreServiceUrl + "/api/diagnostics/create",
                            requestEntity,
                            Map.class
                    );
            Map<String, Object> responseBody = responseEntity.getBody();
            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(responseBody);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при создании отчета: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/files/**")
    public ResponseEntity<byte[]> getFile(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Получаем путь из URL
            String requestURI = request.getRequestURI();
            int filesIndex = requestURI.indexOf("/files/");
            if (filesIndex == -1) {
                return ResponseEntity.notFound().build();
            }
            String filePath = requestURI.substring(filesIndex + 7);
            
            // Декодируем URL для правильной обработки кириллицы и пробелов
            String decodedFilePath = filePath;
            try {
                decodedFilePath = java.net.URLDecoder.decode(filePath, java.nio.charset.StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                // Если декодирование не удалось, используем исходный путь
                System.err.println("Web: Failed to decode path: " + filePath);
            }
            
            // Получаем параметр download
            String downloadParam = request.getParameter("download");
            
            // Кодируем путь для передачи в URL к core-сервису
            // Кодируем каждый сегмент отдельно для правильной обработки пробелов и спецсимволов
            String[] pathSegments = decodedFilePath.split("/");
            StringBuilder encodedPath = new StringBuilder();
            for (int i = 0; i < pathSegments.length; i++) {
                if (i > 0) encodedPath.append("/");
                if (!pathSegments[i].isEmpty()) {
                    // Заменяем + на %20 для правильной обработки пробелов
                    String encoded = java.net.URLEncoder.encode(pathSegments[i], java.nio.charset.StandardCharsets.UTF_8)
                            .replace("+", "%20");
                    encodedPath.append(encoded);
                }
            }
            
            // Проксируем запрос к core-сервису
            String url = coreServiceUrl + "/api/diagnostics/files/" + encodedPath.toString();
            if (downloadParam != null && "true".equals(downloadParam)) {
                url += "?download=true";
            }
            
            System.out.println("Web: Request URI: " + requestURI);
            System.out.println("Web: Decoded filePath: " + decodedFilePath);
            System.out.println("Web: Encoded path: " + encodedPath.toString());
            System.out.println("Web: Calling core service: " + url);
            
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            
            // Определяем Content-Type
            String contentType = "application/octet-stream";
            if (filePath.toLowerCase().endsWith(".jpg") || filePath.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filePath.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filePath.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filePath.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filePath.toLowerCase().endsWith(".doc")) {
                contentType = "application/msword";
            } else if (filePath.toLowerCase().endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));
            
            // Определяем disposition в зависимости от параметра download
            String disposition = (downloadParam != null && "true".equals(downloadParam)) ? "attachment" : "inline";
            String fileName = decodedFilePath.substring(decodedFilePath.lastIndexOf("/") + 1);
            
            // Правильно кодируем имя файла для заголовка Content-Disposition (RFC 5987)
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
            
            headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
            
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("status", status);
            if (eliminationDate != null) body.add("elimination_date", eliminationDate);
            if (conditionAfterElimination != null) body.add("condition_after_elimination", conditionAfterElimination);
            if (responsible != null) body.add("responsible", responsible);
            if (nonEliminationReason != null) body.add("non_elimination_reason", nonEliminationReason);
            if (measures != null) body.add("measures", measures);
            if (comments != null) body.add("comments", comments);
            if (photoResult != null && !photoResult.isEmpty()) {
                body.add("photo_result", photoResult.getResource());
            }
            if (documentResult != null && !documentResult.isEmpty()) {
                body.add("document_result", documentResult.getResource());
            }

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    coreServiceUrl + "/api/diagnostics/" + id,
                    HttpMethod.PUT,
                    requestEntity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(responseEntity.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error updating report: " + e.getMessage());
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            if (detectionDate != null) body.add("detection_date", detectionDate);
            if (diagnosticsType != null) body.add("diagnostics_type", diagnosticsType);
            if (equipment != null) body.add("equipment", equipment);
            if (node != null) body.add("node", node);
            if (area != null) body.add("area", area);
            if (malfunction != null) body.add("malfunction", malfunction);
            if (additionalKit != null) body.add("additional_kit", additionalKit);
            if (causes != null) body.add("causes", causes);
            if (report != null) body.add("report", report);
            if (status != null) body.add("status", status);
            if (eliminationDate != null) body.add("elimination_date", eliminationDate);
            if (conditionAfterElimination != null) body.add("condition_after_elimination", conditionAfterElimination);
            if (responsible != null) body.add("responsible", responsible);
            if (nonEliminationReason != null) body.add("non_elimination_reason", nonEliminationReason);
            if (measures != null) body.add("measures", measures);
            if (comments != null) body.add("comments", comments);
            if (photo != null && !photo.isEmpty()) {
                body.add("photo", photo.getResource());
            }
            if (document != null && !document.isEmpty()) {
                body.add("document", document.getResource());
            }
            if (photoResult != null && !photoResult.isEmpty()) {
                body.add("photo_result", photoResult.getResource());
            }
            if (documentResult != null && !documentResult.isEmpty()) {
                body.add("document_result", documentResult.getResource());
            }

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    coreServiceUrl + "/api/diagnostics/" + id + "/update",
                    HttpMethod.PUT,
                    requestEntity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(responseEntity.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error updating report: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении отчета: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long id) {
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    coreServiceUrl + "/api/diagnostics/" + id,
                    HttpMethod.DELETE,
                    null,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(responseEntity.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error deleting report: " + e.getMessage());
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
            // Для DELETE запроса параметры передаем в URL
            String url = coreServiceUrl + "/api/diagnostics/" + id + "/file?file_type=" + 
                    java.net.URLEncoder.encode(fileType, java.nio.charset.StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(responseEntity.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error deleting file: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при удалении файла: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

