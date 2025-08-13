package ru.georgdeveloper.assistantweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/manuals")
public class ManualController {
    
    @GetMapping
    public String manuals(Model model) {
        return "manuals";
    }
    
    @PostMapping("/upload")
    public String uploadManual(@RequestParam("file") MultipartFile file, Model model) {
        // TODO: Implement manual upload logic
        model.addAttribute("message", "Manual uploaded successfully");
        return "manuals";
    }
    
    @GetMapping("/search")
    @ResponseBody
    public String searchManuals(@RequestParam String query) {
        // TODO: Implement manual search
        return "Search results for: " + query;
    }
}