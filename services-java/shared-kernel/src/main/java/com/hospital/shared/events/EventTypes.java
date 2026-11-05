package com.hospital.shared.events;

/** Canonical Kafka event type names. */
public final class EventTypes {

    public static final String PATIENT_REGISTERED = "PatientRegistered";
    public static final String APPOINTMENT_BOOKED = "AppointmentBooked";
    public static final String APPOINTMENT_CANCELLED = "AppointmentCancelled";
    public static final String ENCOUNTER_COMPLETED = "EncounterCompleted";
    public static final String LAB_RESULT_READY = "LabResultReady";
    public static final String INVOICE_ISSUED = "InvoiceIssued";
    public static final String PAYMENT_COMPLETED = "PaymentCompleted";
    public static final String PAYMENT_FAILED = "PaymentFailed";

    private EventTypes() {
    }
}
