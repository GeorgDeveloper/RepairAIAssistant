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

    @GetMapping("/long_report")
    public String longReport() {
        return "long_report";
    }

    @GetMapping("/manuals")
    public String manuals() {
        return "manuals";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public String processChat(@RequestBody String message) {
        return coreServiceClient.analyzeRepairRequest(message);
    }

    @PostMapping("/api/chat/feedback")
    @ResponseBody
    public String processFeedback(@RequestBody FeedbackDto feedback) {
        return coreServiceClient.sendFeedback(feedback);
    }

    public static class FeedbackDto {
        public String request;
        public String response;
    }
}