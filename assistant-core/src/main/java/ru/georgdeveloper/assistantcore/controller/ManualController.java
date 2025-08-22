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
}
