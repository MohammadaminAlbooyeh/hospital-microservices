package com.hospital.prescription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.hospital.prescription", "com.hospital.shared"})
public class PrescriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrescriptionApplication.class, args);
    }
}
