import http from 'k6/http';
import { check, sleep } from 'k6';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const accessToken = __ENV.ACCESS_TOKEN || '';
const targetVus = Number(__ENV.TARGET_VUS || '100');
const video = open(__ENV.VIDEO_PATH || 'tests/fixtures/sample.mp4', 'b');

export const options = {
  scenarios: {
    uploads: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: Math.min(targetVus, 100) },
        { duration: '30s', target: targetVus },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
};

export default function () {
  const form = new FormData();
  form.append('file', http.file(video, 'sample.mp4', 'video/mp4'));

  const res = http.post(`${baseUrl}/api/v1/videos`, form.body(), {
    headers: {
      'Content-Type': `multipart/form-data; boundary=${form.boundary}`,
      Authorization: `Bearer ${accessToken}`,
    },
    timeout: '120s',
  });

  check(res, {
    'created (201)': (r) => r.status === 201,
    'has video id': (r) => {
      try {
        return typeof r.json('data.id') === 'string';
      } catch (e) {
        return false;
      }
    },
    'sizeBytes > 0': (r) => {
      try {
        return r.json('data.sizeBytes') > 0;
      } catch (e) {
        return false;
      }
    },
  });

  sleep(1);
}
