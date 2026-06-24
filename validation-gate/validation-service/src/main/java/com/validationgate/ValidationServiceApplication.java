package com.validationgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationServiceApplication.class, args);
    }
}
