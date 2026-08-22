package com.vshoba.boutique;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the whole application.
 * Spring Boot scans this package and everything below it,
 * so all @Repository / @Service / @RestController classes
 * are found automatically as long as they live under com.vshoba.boutique.
 */
@SpringBootApplication
public class BoutiqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoutiqueApplication.class, args);
    }
}
