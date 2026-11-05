package com.hospital.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator hospitalRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth", r -> r.path("/api/v1/auth/**").uri("lb://auth-service"))
                .route("patients", r -> r.path("/api/v1/patients/**").uri("lb://patient-service"))
                .route("appointments", r -> r.path("/api/v1/appointments/**", "/api/v1/slots/**").uri("lb://appointment-service"))
                .route("doctors", r -> r.path("/api/v1/doctors/**").uri("lb://doctor-service"))
                .route("departments", r -> r.path("/api/v1/departments/**", "/api/v1/rooms/**", "/api/v1/beds/**").uri("lb://department-service"))
                .route("encounters", r -> r.path("/api/v1/encounters/**").uri("lb://encounter-service"))
                .route("ehr", r -> r.path("/api/v1/ehr/**").uri("lb://ehr-service"))
                .route("prescriptions", r -> r.path("/api/v1/prescriptions/**").uri("lb://prescription-service"))
                .route("labs", r -> r.path("/api/v1/labs/**").uri("lb://lab-service"))
                .route("pharmacy", r -> r.path("/api/v1/pharmacy/**", "/api/v1/stock/**").uri("lb://pharmacy-inventory-service"))
                .route("billing", r -> r.path("/api/v1/billing/**", "/api/v1/invoices/**").uri("lb://billing-service"))
                .route("payments", r -> r.path("/api/v1/payments/**").uri("lb://payment-service"))
                .route("triage", r -> r.path("/api/v1/triage/**").uri("lb://triage-service"))
                .route("insurance", r -> r.path("/api/v1/insurance/**").uri("lb://insurance-service"))
                .route("notifications", r -> r.path("/api/v1/notifications/**").uri("lb://notification-service"))
                .route("analytics", r -> r.path("/api/v1/analytics/**").uri("lb://analytics-service"))
                .route("predictions", r -> r.path("/api/v1/predictions/**").uri("lb://ml-prediction-service"))
                .route("documents", r -> r.path("/api/v1/documents/**").uri("lb://document-service"))
                .route("audit", r -> r.path("/api/v1/audit/**").uri("lb://audit-service"))
                .build();
    }
}
