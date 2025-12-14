package com.Shakwa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableAspectJAutoProxy
@EnableCaching
@EnableScheduling
public class ShakwaApplication {

	/**
	 * Set the default timezone for the entire application to Syria (Asia/Damascus).
	 * This ensures all LocalDateTime.now() calls and audit timestamps use Syrian time.
	 */
	@PostConstruct
	public void initTimezone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Damascus"));
	}

	public static void main(String[] args) {
		// Set timezone early, before Spring context initialization
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Damascus"));
		SpringApplication.run(ShakwaApplication.class, args);
	}

}
