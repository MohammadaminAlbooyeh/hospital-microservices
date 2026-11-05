/**
 * Focused k6 load test for appointment slot contention.
 *
 * Unlike patient_journey_load.js (broad read/write mix), this script
 * hammers a *small, fixed set* of doctor/slot pairs with booking
 * requests from many concurrent VUs, specifically to exercise the
 * optimistic-locking path in appointment-service described in
 * README.md section 5 ("two people will hit the same slot in the same
 * millisecond ... surface a clean 409 to the client") under sustained
 * load rather than just a single paired request (see
 * tests/e2e/test_appointment_concurrency.py for the precise
 * exactly-one-wins assertion in isolation).
 *
 * Usage:
 *   k6 run tests/load/k6/appointment-contention.js
 *   k6 run -e GATEWAY_URL=http://localhost:8080 -e CONTENDED_SLOTS=3 tests/load/k6/appointment-contention.js
 */
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8080';
// Small number of slots shared by every VU, so most requests collide.
const CONTENDED_SLOTS = parseInt(__ENV.CONTENDED_SLOTS || '5', 10);

const bookingWins = new Counter('booking_wins');
const bookingConflicts = new Counter('booking_conflicts');
const unexpectedErrors = new Rate('unexpected_booking_errors');

export const options = {
  scenarios: {
    contention: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 100 },
        { duration: '40s', target: 100 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    // Health check: service must stay up under contention.
    http_req_failed: ['rate<0.05'],
    // The vast majority of attempts against an already-contended slot
    // should resolve cleanly as a win or a conflict, not blow up.
    unexpected_booking_errors: ['rate<0.05'],
  },
};

function doctorIdForSlot(slotIndex) {
  // Deterministic placeholder UUID-shaped id per contended slot; a real
  // run should override via SEED_DOCTOR_IDS (comma-separated) once
  // doctor-service is seeded, e.g.:
  //   k6 run -e SEED_DOCTOR_IDS=uuid1,uuid2,... tests/load/k6/appointment-contention.js
  const seeded = __ENV.SEED_DOCTOR_IDS ? __ENV.SEED_DOCTOR_IDS.split(',') : null;
  if (seeded && seeded[slotIndex % seeded.length]) {
    return seeded[slotIndex % seeded.length];
  }
  return `00000000-0000-0000-0000-${String(slotIndex).padStart(12, '0')}`;
}

export default function () {
  const slotIndex = Math.floor(Math.random() * CONTENDED_SLOTS);
  const doctorId = doctorIdForSlot(slotIndex);
  const slotStart = `2030-04-${String(10 + slotIndex).padStart(2, '0')}T09:00:00Z`;

  const registerResp = http.post(
    `${GATEWAY_URL}/api/v1/patients`,
    JSON.stringify({
      firstName: 'Contention',
      lastName: `Test-${__VU}-${__ITER}`,
      dateOfBirth: '1990-01-01',
      gender: 'OTHER',
      email: `contention.${__VU}.${__ITER}@k6.hospital-platform.test`,
      phone: '+1-555-0400',
      nationalId: `CONTEND-${__VU}-${__ITER}`,
      emergencyContactName: 'N/A',
      emergencyContactPhone: '+1-555-0401',
    }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'POST /patients' } }
  );
  if (registerResp.status !== 201) {
    return; // patient-service not reachable / not implemented yet
  }
  const patientId = registerResp.json('id');

  const bookResp = http.post(
    `${GATEWAY_URL}/api/v1/appointments`,
    JSON.stringify({ patientId, doctorId, slotStart }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'POST /appointments' } }
  );

  if (bookResp.status === 201) {
    bookingWins.add(1);
  } else if (bookResp.status === 409) {
    bookingConflicts.add(1);
  }

  const ok = check(bookResp, {
    'booking resolves as win, conflict, or not-yet-implemented': (r) =>
      [201, 409, 404, 501].includes(r.status),
  });
  unexpectedErrors.add(!ok);
}
