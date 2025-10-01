package ru.georgdeveloper.assistantweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BdAvWebController {

    @GetMapping("/bdav")
    public String bdav() {
        return "bd_av";
    }
}


