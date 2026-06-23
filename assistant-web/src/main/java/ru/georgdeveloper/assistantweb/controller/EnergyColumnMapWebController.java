package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Прокси к assistant-core: карты колонок, импорт Excel, выборка суточных значений.
 */
@RestController
@RequestMapping("/api/energy")
public class EnergyColumnMapWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public EnergyColumnMapWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/column-map")
    public List<String> listColumnMaps() {
        return restTemplate.getForObject(coreServiceUrl + "/api/energy/column-map", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/column-map/{resource}")
    public Map<String, Object> getColumnMap(@PathVariable String resource) {
        return restTemplate.getForObject(
                coreServiceUrl + "/api/energy/column-map/" + resource,
                Map.class);
    }

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("year") int year,
            @RequestParam(value = "resource", required = false) String resource)
            throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource =
                new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.xlsx";
                    }
                };
        body.add("file", fileResource);
        body.add("year", Integer.toString(year));
        if (resource != null && !resource.isBlank()) {
            body.add("resource", resource);
        }
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(coreServiceUrl + "/api/energy/import", request, Map.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/daily-values")
    public List<Map<String, Object>> dailyValues(
            @RequestParam("resource") String resource,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String url =
                UriComponentsBuilder.fromUriString(coreServiceUrl + "/api/energy/daily-values")
                        .queryParam("resource", resource)
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .toUriString();
        return restTemplate.getForObject(url, List.class);
    }
}
