package com.hospital.patient.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class InsuranceClient {

    private final RestClient restClient;

    public InsuranceClient(RestClient.Builder builder,
                           @Value("${hospital.clients.insurance-url:http://insurance-service:8002}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public Map<String, Object> checkEligibility(String policyNumber, String memberId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("policy_number", policyNumber);
        request.put("member_id", memberId);
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/v1/insurance/eligibility")
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return response == null ? Map.of("eligible", false) : response;
        } catch (Exception ex) {
            log.warn("Insurance eligibility check failed: {}", ex.getMessage());
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("eligible", false);
            fallback.put("reason", "insurance service unavailable");
            return fallback;
        }
    }
}
