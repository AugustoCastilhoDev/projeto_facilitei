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
  // 1 worker: os specs compartilham o mesmo backend/Postgres reais (nao
  // mocks), e rodar arquivos diferentes em paralelo mostrou flakiness (dois
  // Chromium concorrentes disputando CPU atrasam o Angular o suficiente pra
  // um fill() de formulario acontecer antes da change detection anterior
  // assentar) - descoberto ao adicionar o segundo spec file da suite.
  workers: 1,
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
