# End-to-end tests

HTTP-based end-to-end tests that drive the platform the same way a real
client would: through `api-gateway`, over plain REST, with no mocking and
no direct database or broker access. Consistent with `services-python`'s
use of `pytest` + `httpx`.

## Running

1. Start the stack from the repo root:

   ```bash
   make up
   # or: docker compose up -d --build
   ```

2. (Optional) Seed reference data (doctors/departments) so journey tests
   have something to book against:

   ```bash
   python3 tests/fixtures/seed-data/seed.py
   ```

3. Install test dependencies and run:

   ```bash
   pip install pytest httpx
   pytest tests/e2e -v
   ```

## Configuration

| Env var       | Default                 | Meaning                              |
|---------------|--------------------------|---------------------------------------|
| `GATEWAY_URL` | `http://localhost:8080` | Base URL of `api-gateway`             |
| `E2E_TIMEOUT` | `10`                    | Per-request timeout, in seconds       |

## What's covered

- **`test_patient_journey.py`** - the full happy path from README.md
  section 4: register -> browse doctors/slots -> book appointment ->
  check-in (encounter) -> lab order + result -> discharge/invoice ->
  payment success. Each step is skipped (not failed) if the service
  backing it isn't implemented yet, so the suite stays meaningful while
  services are still being scaffolded.
- **`test_appointment_concurrency.py`** - fires two simultaneous booking
  requests at the same doctor/slot and asserts exactly one gets `201`
  and the other a clean `409`, proving out the optimistic-locking
  (`@Version`) behavior described in README.md section 5. It also checks
  a sequential follow-up booking against the now-taken slot is rejected
  too.

Tests are intentionally tolerant of partially-implemented services
(treating `404`/`501`/`502`/`503` on a not-yet-built endpoint as a skip
rather than a failure) since several services in this repo are still
skeletons. As each service's REST surface lands, the corresponding step
starts asserting for real instead of skipping.
