import { defineConfig, devices } from '@playwright/test';

/**
 * So o frontend e auto-iniciado (comando simples, sem dependencias externas).
 * Backend e Postgres precisam estar rodando antes de executar a suite - ver
 * e2e/README.md para os pre-requisitos completos.
 */
export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm start',
    cwd: '../frontend',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
