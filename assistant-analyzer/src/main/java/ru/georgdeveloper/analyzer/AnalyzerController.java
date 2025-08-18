package ru.georgdeveloper.analyzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/analyzer")
public class AnalyzerController {
    
    @Autowired
    private AnalyzerService analyzerService;
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не выбран");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Поддерживаются только CSV файлы");
        }
        
        try {
            // Сохранение файла
            String fileName = filename;
            File uploadFile = new File("uploads/" + fileName);
            file.transferTo(uploadFile);
            
            // Анализ файла
            String result = analyzerService.processEquipmentFile(fileName);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                               .body("Ошибка загрузки файла: " + e.getMessage());
        }
    }
    
    @PostMapping("/analyze/{fileName}")
    public ResponseEntity<List<EquipmentAnalyzer.RepairInstruction>> analyzeFile(@PathVariable String fileName) {
        try {
            List<EquipmentAnalyzer.RepairInstruction> instructions = 
                analyzerService.analyzeEquipmentFile(fileName);
            return ResponseEntity.ok(instructions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analyzer module is running");
    }
}