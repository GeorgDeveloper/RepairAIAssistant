package ru.georgdeveloper.assistantcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AssistantCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssistantCoreApplication.class, args);
	}

}
