package com.hospital.billing.domain.model;

import com.hospital.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends BaseEntity {

    private UUID encounterId;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal insurerShare;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal patientShare;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal insuranceCoveragePercent;

    private Instant issuedAt;

    private Instant paidAt;

    @Column(length = 1024)
    private String failureReason;

    private UUID lastPaymentId;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceLine> lines = new ArrayList<>();

    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        this.lines.add(line);
    }
}
