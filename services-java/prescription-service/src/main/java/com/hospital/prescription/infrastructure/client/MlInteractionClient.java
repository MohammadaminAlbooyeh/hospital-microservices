package com.hospital.prescription.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MlInteractionClient {

    private final RestClient restClient;

    public MlInteractionClient(RestClient.Builder builder,
                               @Value("${hospital.clients.ml-prediction-url:http://ml-prediction-service:8005}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(500);
        requestFactory.setReadTimeout(1000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public void check(List<String> drugCodes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("drugCodes", drugCodes);
        try {
            restClient.post()
                    .uri("/api/v1/predictions/drug-interactions")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("ML drug interaction prediction unavailable: {}", ex.getMessage());
        }
    }
}
