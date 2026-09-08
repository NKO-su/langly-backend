package com.langly.langly_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LanglyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LanglyBackendApplication.class, args);
	}

}
