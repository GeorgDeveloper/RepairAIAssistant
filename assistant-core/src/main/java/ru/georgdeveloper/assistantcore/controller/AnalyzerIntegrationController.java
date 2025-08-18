package ru.georgdeveloper.assistantcore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/equipment")
public class AnalyzerIntegrationController {
    

    
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeEquipment(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || filename == null || !filename.endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Требуется CSV файл");
        }
        
        return ResponseEntity.ok("Анализ CSV файлов временно недоступен");
    }
    
    @GetMapping("/instructions/{fileName}")
    public ResponseEntity<String> getInstructions(@PathVariable String fileName) {
        return ResponseEntity.ok("Получение инструкций временно недоступно");
    }
}