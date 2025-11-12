import { defineConfig, devices } from '@playwright/test';

const PORT = process.env.E2E_PORT ? Number(process.env.E2E_PORT) : 4173;
const HOST = process.env.E2E_HOST || '127.0.0.1';

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 120000,
  expect: {
    timeout: 15000,
  },
  retries: process.env.CI ? 2 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://${HOST}:${PORT}`,
    trace: 'on-first-retry',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: `npm run dev -- --host=${HOST} --port=${PORT}`,
    url: `http://${HOST}:${PORT}`,
    reuseExistingServer: !process.env.CI,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});

