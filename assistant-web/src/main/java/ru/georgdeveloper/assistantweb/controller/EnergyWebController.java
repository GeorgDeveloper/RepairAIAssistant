package ru.georgdeveloper.assistantweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class EnergyWebController {

    @GetMapping("/energy/water")
    public String waterPage(Model model) {
        return energyPage(model, "WATER", "Вода");
    }

    @GetMapping("/energy/gas")
    public String gasPage(Model model) {
        return energyPage(model, "GAS", "Газ");
    }

    @GetMapping("/energy/electricity")
    public String electricityPage(Model model) {
        return energyPage(model, "ELECTRICITY", "Эл. энергия");
    }

    @GetMapping("/energy/nitrogen")
    public String nitrogenPage(Model model) {
        return energyPage(model, "NITROGEN", "Азот");
    }

    @GetMapping("/energy/steam")
    public String steamPage(Model model) {
        return energyPage(model, "STEAM", "Пар");
    }

    @GetMapping("/energy/total")
    public String totalPage(Model model) {
        return energyPage(model, "TOTAL", "Итого");
    }

    @GetMapping("/energy/import")
    public String importPage() {
        return "energy_import";
    }

    private String energyPage(Model model, String resourceCode, String titleRu) {
        model.addAttribute("energyResourceCode", resourceCode);
        model.addAttribute("energyTitleRu", titleRu);
        return "energy_resource";
    }
}
