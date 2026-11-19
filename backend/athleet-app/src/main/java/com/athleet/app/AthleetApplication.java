package com.athleet.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.athleet")
@EntityScan("com.athleet")
@EnableJpaRepositories("com.athleet")
@EnableScheduling
public class AthleetApplication {

    public static void main(String[] args) {
        SpringApplication.run(AthleetApplication.class, args);
    }
}
