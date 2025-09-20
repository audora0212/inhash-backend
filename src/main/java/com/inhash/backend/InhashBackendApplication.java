package com.inhash.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InhashBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(InhashBackendApplication.class, args);
    }
}


