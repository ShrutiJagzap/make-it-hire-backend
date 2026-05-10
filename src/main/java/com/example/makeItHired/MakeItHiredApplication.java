package com.example.makeItHired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class MakeItHiredApplication {

	private static final Logger log = LoggerFactory.getLogger(MakeItHiredApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(MakeItHiredApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void initializeDirectories() {
		try {
			// Create upload directories for Render's /tmp storage
			Files.createDirectories(Paths.get("/tmp/uploads/resumes"));
			Files.createDirectories(Paths.get("/tmp/uploads/id_photos"));
			Files.createDirectories(Paths.get("/tmp/uploads/profile"));
			log.info("✅ Upload directories created successfully");
		} catch (IOException e) {
			log.warn("⚠️ Could not create upload directories: {}", e.getMessage());
		}
	}

}
