package com.hospital.auth.infrastructure.messaging;

import com.hospital.auth.domain.model.Role;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.shared.events.EventTypes;
import com.hospital.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private final OutboxService outboxService;

    public void userRegistered(UserAccount user) {
        if (!user.hasRole(Role.PATIENT)) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("firstName", user.getFirstName());
        payload.put("lastName", user.getLastName());
        payload.put("phone", user.getPhone());
        payload.put("roles", user.roleList());
        outboxService.publish("UserAccount", user.getId().toString(), EventTypes.PATIENT_REGISTERED, payload);
    }
}
