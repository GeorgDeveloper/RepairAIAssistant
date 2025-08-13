package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantcore.repository.ManualRepository;
import ru.georgdeveloper.assistantcore.model.Manual;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ManualStorageService {
    
    @Autowired
    private ManualRepository manualRepository;
    
    public void storeManual(String manualContent, String deviceType) {
        Manual manual = new Manual();
        manual.setContent(manualContent);
        manual.setDeviceType(deviceType);
        manualRepository.save(manual);
    }
    
    public List<String> searchManuals(String query) {
        return manualRepository.findByContentContaining(query)
                .stream()
                .map(Manual::getContent)
                .collect(Collectors.toList());
    }
}