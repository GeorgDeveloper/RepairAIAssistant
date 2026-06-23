package ru.georgdeveloper.assistantweb.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/production-days-correction")
public class ProductionDaysCorrectionLoginController {

    private static final String SESSION_ATTR = "pdc_authenticated";

    @Value("${app.production-days-correction.password:kadmin}")
    private String expectedPassword;

    @GetMapping("/login")
    public String loginPage() {
        return "production_days_correction_login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("password") String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (expectedPassword != null && expectedPassword.equals(password)) {
            session.setAttribute(SESSION_ATTR, true);
            return "redirect:/production-days-correction";
        }
        redirectAttributes.addFlashAttribute("error", "Неверный пароль");
        return "redirect:/production-days-correction/login";
    }
}
