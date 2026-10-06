package ru.georgdeveloper.assistantweb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/energy/steam-hourly")
public class EnergySteamHourlyWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EnergySteamHourlyWebController(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/hourly-values")
    public ResponseEntity<?> hourlyValues(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String url = UriComponentsBuilder.fromUriString(coreServiceUrl + "/api/energy/steam-hourly/hourly-values")
                .queryParam("from", from)
                .queryParam("to", to)
                .toUriString();
        try {
            return ResponseEntity.ok(restTemplate.getForObject(url, List.class));
        } catch (HttpStatusCodeException ex) {
            return forwardError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/range")
    public ResponseEntity<?> dataRange() {
        try {
            return ResponseEntity.ok(
                    restTemplate.getForObject(coreServiceUrl + "/api/energy/steam-hourly/range", Map.class));
        } catch (HttpStatusCodeException ex) {
            return forwardError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
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
        try {
            Map<String, Object> result = restTemplate.postForObject(
                    coreServiceUrl + "/api/energy/steam-hourly/import",
                    new HttpEntity<>(body, headers),
                    Map.class);
            return ResponseEntity.ok(result);
        } catch (HttpStatusCodeException ex) {
            return forwardError(ex);
        }
    }

    private ResponseEntity<Map<String, Object>> forwardError(HttpStatusCodeException ex) {
        String raw = ex.getResponseBodyAsString();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(raw, Map.class);
            if (parsed.containsKey("error") || parsed.containsKey("message")) {
                return ResponseEntity.status(ex.getStatusCode()).body(parsed);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return ResponseEntity.status(ex.getStatusCode() != null ? ex.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", raw != null && !raw.isBlank() ? raw : ex.getMessage()));
    }
}
