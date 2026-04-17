package ru.georgdeveloper.assistantyandexbot.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Component
public class CoreServiceClient {

	private final RestTemplate restTemplate;
	private final String coreServiceBaseUrl;

	public CoreServiceClient(RestTemplate restTemplate,
			@Value("${core.service.url:http://localhost:8080/api}") String coreServiceBaseUrl) {
		this.restTemplate = restTemplate;
		this.coreServiceBaseUrl = coreServiceBaseUrl.replaceAll("/$", "");
	}

	public String analyzeRepairRequest(String request) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
		HttpEntity<String> entity = new HttpEntity<>(request, headers);
		return restTemplate.postForObject(coreServiceBaseUrl + "/analyze", entity, String.class);
	}

	public void saveFeedback(String request, String response) {
		FeedbackDto dto = new FeedbackDto(request, response);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<FeedbackDto> entity = new HttpEntity<>(dto, headers);
		restTemplate.postForObject(coreServiceBaseUrl + "/feedback", entity, String.class);
	}
}
