package com.hospital.ehr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.hospital.ehr", "com.hospital.shared"})
public class EhrApplication {

    public static void main(String[] args) {
        SpringApplication.run(EhrApplication.class, args);
    }
}
