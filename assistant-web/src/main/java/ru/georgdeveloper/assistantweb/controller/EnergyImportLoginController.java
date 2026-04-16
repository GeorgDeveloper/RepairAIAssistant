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
@RequestMapping("/energy/import")
public class EnergyImportLoginController {

    private static final String SESSION_ATTR = "energy_import_authenticated";

    @Value("${app.energy-import.password:eadmin}")
    private String expectedPassword;

    @GetMapping("/login")
    public String loginPage() {
        return "energy_import_login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("password") String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (expectedPassword != null && expectedPassword.equals(password)) {
            session.setAttribute(SESSION_ATTR, true);
            return "redirect:/energy/import";
        }
        redirectAttributes.addFlashAttribute("error", "Неверный пароль");
        return "redirect:/energy/import/login";
    }
}
