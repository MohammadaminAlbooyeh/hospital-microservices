package com.hospital.shared.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEntry, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEntry> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
