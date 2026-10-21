package com.nabta.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NabtaBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(NabtaBackendApplication.class, args);
    }
}