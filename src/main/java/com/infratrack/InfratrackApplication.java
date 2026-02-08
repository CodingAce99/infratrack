package com.infratrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for Infratrack.
 * 
 * @EnableAsync allows methods to run asynchronously (needed for SSH operations)
 */
@SpringBootApplication
@EnableAsync
public class InfratrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfratrackApplication.class, args);
    }

}
