package com.hospital.encounter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.hospital.encounter", "com.hospital.shared"})
public class EncounterApplication {

    public static void main(String[] args) {
        SpringApplication.run(EncounterApplication.class, args);
    }
}
