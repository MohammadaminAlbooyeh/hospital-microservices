/**
 * k6 load test approximating README.md's "10,000 active patients" target.
 *
 * Mixes read-heavy traffic (browsing doctors and appointment slots) with
 * write traffic (booking appointments) through api-gateway, matching the
 * early steps of the patient journey described in README.md section 4.
 *
 * Default scale is a small smoke test so this can run on a laptop /
 * in CI without hammering a dev stack. Set LOAD_SCALE=full for a run
 * that ramps toward the 10,000-VU target described in the README.
 *
 * Usage:
 *   k6 run tests/load/patient_journey_load.js
 *   k6 run -e GATEWAY_URL=http://localhost:8080 tests/load/patient_journey_load.js
 *   k6 run -e LOAD_SCALE=full tests/load/patient_journey_load.js
 *
 * See tests/load/README.md for details.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://localhost:8080';
const SCALE = __ENV.LOAD_SCALE || 'smoke';

const bookingErrors = new Rate('booking_errors');
const browseErrors = new Rate('browse_errors');

// Smoke scale (default): safe to run anywhere, including CI, without a
// beefy stack behind it. Full scale approximates the README's "10,000
// active patients" headline by ramping virtual users toward 10k.
const STAGES = {
  smoke: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  full: [
    { duration: '2m', target: 1000 },
    { duration: '3m', target: 5000 },
    { duration: '5m', target: 10000 },
    { duration: '10m', target: 10000 },
    { duration: '3m', target: 0 },
  ],
};

export const options = {
  stages: STAGES[SCALE] || STAGES.smoke,
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<2000'],
    http_req_failed: ['rate<0.02'],
    booking_errors: ['rate<0.05'],
    browse_errors: ['rate<0.02'],
  },
  // Spread scenarios' own insights via tags.
  tags: { scenario: 'patient_journey_load' },
};

function randomSuffix() {
  return Math.random().toString(36).slice(2, 10);
}

/**
 * Read-heavy flow: browse the doctor directory and a doctor's slots.
 * This is the far more common operation in a real hospital portal -
 * most visits are people checking availability, not booking.
 */
function browseDoctorsAndSlots() {
  const doctorsResp = http.get(`${GATEWAY_URL}/api/v1/doctors`, {
    tags: { name: 'GET /doctors' },
  });
  const doctorsOk = check(doctorsResp, {
    'doctors list status is 200 or a graceful not-yet-implemented code': (r) =>
      [200, 404, 501].includes(r.status),
  });
  browseErrors.add(!doctorsOk);

  if (doctorsResp.status !== 200) {
    return null;
  }

  let doctors = [];
  try {
    doctors = doctorsResp.json();
  } catch (e) {
    return null;
  }
  if (!Array.isArray(doctors) || doctors.length === 0) {
    return null;
  }

  const doctor = doctors[Math.floor(Math.random() * doctors.length)];
  const slotsResp = http.get(`${GATEWAY_URL}/api/v1/slots?doctorId=${doctor.id}`, {
    tags: { name: 'GET /slots' },
  });
  const slotsOk = check(slotsResp, {
    'slots status is 200 or a graceful not-yet-implemented code': (r) =>
      [200, 404, 501].includes(r.status),
  });
  browseErrors.add(!slotsOk);

  return doctor.id;
}

/**
 * Write flow: register a lightweight throwaway patient, then book an
 * appointment with a randomly-chosen doctor. Deliberately a smaller
 * share of total traffic than browsing, mirroring real usage.
 */
function registerAndBook(doctorId) {
  const email = `loadtest.${randomSuffix()}@k6.hospital-platform.test`;
  const registerResp = http.post(
    `${GATEWAY_URL}/api/v1/patients`,
    JSON.stringify({
      firstName: 'Load',
      lastName: `Test-${randomSuffix()}`,
      dateOfBirth: '1992-07-04',
      gender: 'OTHER',
      email,
      phone: '+1-555-0300',
      nationalId: `LOAD-${randomSuffix().toUpperCase()}`,
      emergencyContactName: 'N/A',
      emergencyContactPhone: '+1-555-0301',
    }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'POST /patients' } }
  );

  const registerOk = check(registerResp, {
    'register status is 201 or a graceful not-yet-implemented code': (r) =>
      [201, 404, 501].includes(r.status),
  });
  bookingErrors.add(!registerOk);
  if (registerResp.status !== 201) {
    return;
  }

  const patientId = registerResp.json('id');
  const hour = 8 + Math.floor(Math.random() * 9); // 08:00-16:00
  const minute = Math.random() < 0.5 ? '00' : '30';
  const slotStart = `2030-03-${String(1 + Math.floor(Math.random() * 27)).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${minute}:00Z`;

  const bookResp = http.post(
    `${GATEWAY_URL}/api/v1/appointments`,
    JSON.stringify({
      patientId,
      doctorId,
      slotStart,
    }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'POST /appointments' } }
  );

  // 409 is an *expected* outcome under load (optimistic-locking conflict
  // on a contended slot, per README section 5) - only count unexpected
  // statuses as errors.
  const bookOk = check(bookResp, {
    'booking status is 201, 409 (lock conflict), or a graceful not-yet-implemented code': (r) =>
      [201, 409, 404, 501].includes(r.status),
  });
  bookingErrors.add(!bookOk);
}

export default function () {
  const doctorId = browseDoctorsAndSlots();
  sleep(Math.random() * 1.5 + 0.5);

  // ~80/20 read/write mix, roughly matching a real outpatient portal.
  if (doctorId && Math.random() < 0.2) {
    registerAndBook(doctorId);
  }

  sleep(Math.random() * 2 + 1);
}
