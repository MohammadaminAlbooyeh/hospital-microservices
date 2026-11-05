-- One logical database per service (database-per-service pattern).
-- Executed automatically by the postgres image on first start.

CREATE DATABASE hospital_auth;
CREATE DATABASE hospital_patient;
CREATE DATABASE hospital_appointment;
CREATE DATABASE hospital_doctor;
CREATE DATABASE hospital_department;
CREATE DATABASE hospital_encounter;
CREATE DATABASE hospital_ehr;
CREATE DATABASE hospital_prescription;
CREATE DATABASE hospital_lab;
CREATE DATABASE hospital_pharmacy;
CREATE DATABASE hospital_billing;
CREATE DATABASE hospital_payment;
CREATE DATABASE hospital_audit;
CREATE DATABASE hospital_insurance;
CREATE DATABASE hospital_triage;
CREATE DATABASE hospital_analytics;
CREATE DATABASE hospital_notification;
CREATE DATABASE hospital_document;

GRANT ALL PRIVILEGES ON DATABASE hospital_auth TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_patient TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_appointment TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_doctor TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_department TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_encounter TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_ehr TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_prescription TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_lab TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_pharmacy TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_billing TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_payment TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_audit TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_insurance TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_triage TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_analytics TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_notification TO hospital;
GRANT ALL PRIVILEGES ON DATABASE hospital_document TO hospital;
