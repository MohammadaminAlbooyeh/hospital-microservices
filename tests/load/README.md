# Load tests (k6)

Two scripts, both under `tests/load/k6/`:

- **`patient_journey_load.js`** - broad, realistic traffic against
  `api-gateway`: mostly reads (browsing doctors and their open slots),
  with a smaller slice of writes (patient registration + appointment
  booking), approximating README.md's "10,000 active patients" target
  for `appointment-service`.
- **`appointment-contention.js`** - narrow and adversarial: many VUs
  repeatedly booking a small, fixed pool of doctor/slot pairs, to drive
  sustained optimistic-locking contention in `appointment-service`
  rather than incidental collisions.

## Install k6

```bash
brew install k6          # macOS
# or see https://k6.io/docs/get-started/installation/
```

## Run

Default ("smoke") scale ramps to 50 VUs over ~2 minutes - safe for a
laptop or CI against a local docker-compose stack:

```bash
k6 run tests/load/k6/patient_journey_load.js
```

Point it at a non-default gateway:

```bash
k6 run -e GATEWAY_URL=http://localhost:8080 tests/load/k6/patient_journey_load.js
```

Full scale ramps toward the 10,000-VU figure from the README (roughly a
23-minute run; only point this at a stack sized to take it):

```bash
k6 run -e LOAD_SCALE=full tests/load/k6/patient_journey_load.js
```

Run the focused contention script (defaults to 100 VUs over ~70s against
5 fixed slots):

```bash
k6 run tests/load/k6/appointment-contention.js
# or against real seeded doctor ids:
k6 run -e SEED_DOCTOR_IDS=<uuid1>,<uuid2> tests/load/k6/appointment-contention.js
```

## Thresholds

The script fails the run (non-zero exit) if, across the whole run:

- p95 request latency exceeds 800ms, or p99 exceeds 2000ms
- more than 2% of HTTP requests fail outright
- more than 5% of booking attempts fail with something other than a
  success or an expected `409` optimistic-locking conflict
- more than 2% of browse (doctor/slot listing) requests fail

`409` responses from `/api/v1/appointments` are treated as a *correct*
outcome, not an error - two patients legitimately contending for the
same slot is exactly the scenario README.md section 5 describes, and
one of them should be rejected cleanly rather than corrupting state.

Any endpoint that returns `404`/`501` (not yet implemented by a service
that is still being scaffolded) is also treated as non-fatal, so this
script keeps producing useful signal as services land incrementally.
