package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.georgdeveloper.assistantcore.model.Manual;
import ru.georgdeveloper.assistantcore.repository.ManualRepository;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manuals")
public class ManualController {

    @Autowired
    private ManualRepository manualRepository;

    @GetMapping("/search")
    @ResponseBody
    public List<Manual> searchManuals(@RequestParam String query) {
        return manualRepository.findByContentContaining(query);
    }

    @GetMapping("/all")
    @ResponseBody
    public List<Manual> getAllManuals() {
        return manualRepository.findAll();
    }

    @PostMapping("/upload")
    @ResponseBody
    public String uploadManual(@RequestParam("file") MultipartFile file, @RequestParam String region, @RequestParam String equipment, @RequestParam String node, @RequestParam String deviceType, @RequestParam String content) {
        try {
            Manual manual = new Manual();
            manual.setRegion(region);
            manual.setEquipment(equipment);
            manual.setNode(node);
            manual.setDeviceType(deviceType);
            manual.setContent(content);
            manual.setFileName(file.getOriginalFilename());
            manual.setFiles(file.getBytes()); // Save file content in 'files' column
            manualRepository.save(manual);
            return "Документ успешно загружен";
        } catch (Exception e) {
            return "Ошибка загрузки документа: " + e.getMessage();
        }
    }

    @GetMapping("/view/{id}")
    @ResponseBody
    public ResponseEntity<?> viewManual(@PathVariable Long id) {
        return manualRepository.findById(id)
                .map(manual -> {
                    if (manual.getFiles() == null || manual.getFiles().length == 0) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("Файл не найден");
                    }

                    HttpHeaders headers = new HttpHeaders();
                    String fileName = manual.getFileName();
                    MediaType mediaType = getMediaType(fileName);
                    headers.setContentType(mediaType);
                    // Не устанавливаем Content-Disposition: attachment, чтобы браузер мог отобразить файл
                    return new ResponseEntity<>(manual.getFiles(), headers, HttpStatus.OK);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Файл не найден"));
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<?> downloadManual(@PathVariable Long id) {
        return manualRepository.findById(id)
                .map(manual -> {
                    if (manual.getFiles() == null || manual.getFiles().length == 0) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("Файл не найден");
                    }

                    HttpHeaders headers = new HttpHeaders();
                    String encodedFileName;
                    try {
                        encodedFileName = URLEncoder.encode(manual.getFileName(), StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        encodedFileName = "file"; // Fallback filename
                    }
                    headers.setContentDisposition(ContentDisposition.builder("attachment")
                            .filename(encodedFileName, StandardCharsets.UTF_8)
                            .build());
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    return new ResponseEntity<>(manual.getFiles(), headers, HttpStatus.OK);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Файл не найден"));
    }

    private MediaType getMediaType(String fileName) {
        if (fileName == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (lowerFileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lowerFileName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else if (lowerFileName.endsWith(".bmp")) {
            return MediaType.valueOf("image/bmp");
        } else if (lowerFileName.endsWith(".webp")) {
            return MediaType.valueOf("image/webp");
        } else if (lowerFileName.endsWith(".doc")) {
            return MediaType.valueOf("application/msword");
        } else if (lowerFileName.endsWith(".docx")) {
            return MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

        // Добавлены эндпоинты для удаления и обновления записей


    // Для совместимости с фронтендом
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteManual(@PathVariable Long id) {
        manualRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/manuals/{id}")
    public ResponseEntity<Void> updateManual(@PathVariable int id, @RequestBody Map<String, Object> manual) {
           
        manualRepository.updateByParametr(
            (long) id,
            (String) manual.get("region"),
            (String) manual.get("equipment"),
            (String) manual.get("node"),
            (String) manual.get("deviceType"),
            (String) manual.get("content")
        );
        return ResponseEntity.noContent().build();
    }
}
