package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantweb.client.CoreServiceClient;

@Controller
public class AssistantController {
    
    @Autowired
    private CoreServiceClient coreServiceClient;
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
    
    @PostMapping("/api/chat")
    @ResponseBody
    public String processChat(@RequestBody String message) {
        return coreServiceClient.analyzeRepairRequest(message);
    }
}