package ru.georgdeveloper.assistantweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/manuals")
public class ManualsWebController {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;

    private final RestTemplate restTemplate;

    public ManualsWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/all")
    public List<Map<String, Object>> all() {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/manuals/all", List.class);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String query) {
        return (List<Map<String, Object>>) (List<?>) restTemplate.getForObject(coreServiceUrl + "/manuals/search?query=" + query, List.class);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestPart("file") MultipartFile file,
                         @RequestPart("region") String region,
                         @RequestPart("equipment") String equipment,
                         @RequestPart("node") String node,
                         @RequestPart("deviceType") String deviceType,
                         @RequestPart("content") String content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(getBytes(file)) {
            @Override
            public String getFilename() { return file.getOriginalFilename(); }
        });
        body.add("region", region);
        body.add("equipment", equipment);
        body.add("node", node);
        body.add("deviceType", deviceType);
        body.add("content", content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(coreServiceUrl + "/manuals/upload", requestEntity, String.class);
        return response.getBody();
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(coreServiceUrl + "/manuals/download/" + id, byte[].class);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(response.getHeaders());
        return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        restTemplate.delete(coreServiceUrl + "/manuals/delete/" + id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/manuals/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id, @RequestBody Map<String, Object> manual) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(manual, headers);
        restTemplate.exchange(coreServiceUrl + "/manuals/manuals/" + id, HttpMethod.PUT, entity, Void.class);
        return ResponseEntity.noContent().build();
    }

    private byte[] getBytes(MultipartFile file) {
        try { return file.getBytes(); } catch (Exception e) { throw new RuntimeException(e); }
    }
}


