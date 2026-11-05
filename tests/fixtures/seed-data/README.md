# Seed data

Sample dev/test data for local work against a running docker-compose
stack: a handful of departments, doctors, patients, and the appointments
that tie them together. Field names follow the entities implemented so
far (see `services-java/patient-service`'s `PatientRequest` DTO for the
patient shape) and the events in `contracts/avro/` for id/field naming
conventions used by services that are still being scaffolded
(doctor-service, department-service, appointment-service).

## Files

- `departments.json` - 5 hospital departments
- `doctors.json` - 5 doctors, one per department, keyed by `licenseNumber`
- `patients.json` - 5 patients, keyed by `nationalId`
- `appointments.json` - 3 appointments linking the above by natural key

## Loading

Seed via the real REST APIs (through `api-gateway`), so normal service
validation runs - this is the preferred approach per the task brief,
over inserting rows directly into Postgres:

```bash
make up                       # start the stack
python3 tests/fixtures/seed-data/seed.py
```

`GATEWAY_URL` (default `http://localhost:8080`) and `SEED_TIMEOUT`
(default `10` seconds) can be overridden via environment variables.

The script is safe to re-run: a `409`/`422` on a record that already
exists is reported and skipped rather than treated as an error, and any
endpoint not implemented yet (`404`/`501`) is skipped with a note
instead of failing the whole run - several of the services this data
targets (doctor-service, department-service, appointment-service) are
still skeletons as of this writing.
