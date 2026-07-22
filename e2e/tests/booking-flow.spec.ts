import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { Page, expect, test } from '@playwright/test';

const CPF_TESTE = '52998224725';

function slugUnico(prefixo: string): string {
  return `${prefixo}-${Date.now()}`;
}

function dataDeAmanha(): string {
  const amanha = new Date();
  amanha.setDate(amanha.getDate() + 1);
  return amanha.toISOString().slice(0, 10);
}

/**
 * Modelo BYOPP: a cobranca Pix do sinal sai da PROPRIA conta Asaas do tenant
 * (ver TenantAsaasConfigController), nao mais de uma chave global da
 * plataforma. Para o teste "com sinal" gerar uma cobranca real, reaproveita
 * a mesma chave sandbox ja usada pelo backend local (application-local.yml,
 * git-ignorado) - nao ha diferenca pratica entre "a chave da plataforma" e
 * "a chave do tenant" nesse ambiente de teste, ja que so existe uma conta
 * sandbox disponivel. Se o arquivo nao existir, o teste e pulado.
 */
function lerChaveAsaasLocal(): string | null {
  try {
    const caminho = resolve(__dirname, '../../backend/src/main/resources/application-local.yml');
    const conteudo = readFileSync(caminho, 'utf8');
    const match = conteudo.match(/api-key:\s*(\S+)/);
    return match ? match[1] : null;
  } catch {
    return null;
  }
}

async function configurarChaveAsaas(page: Page, apiKey: string): Promise<void> {
  await page.getByRole('tab', { name: 'Pagamentos' }).click();
  await page.getByLabel('Chave da API Asaas (da sua conta)').fill(apiKey);
  await page.getByRole('button', { name: 'Salvar chave' }).click();
  await expect(page.getByText('Recebimento configurado')).toBeVisible();
}

const NOME_PROFISSIONAL = 'Profissional Teste';

/**
 * Cria um profissional na aba "Profissionais" e vincula o servico informado
 * a ele - desde o modelo de multiplos profissionais, gerar horarios exige um
 * profissional que realize aquele servico (ver ProfissionalAdminController).
 */
async function cadastrarProfissional(page: Page, nomeServico: string): Promise<void> {
  await page.getByRole('tab', { name: 'Profissionais' }).click();
  await page.getByRole('button', { name: 'Novo profissional' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Nome').fill(NOME_PROFISSIONAL);
  await dialog.getByLabel('Abre as').fill('08:00');
  await dialog.getByLabel('Fecha as').fill('20:00');
  await dialog.getByLabel('Servicos que realiza').click();
  await page.getByRole('option', { name: nomeServico }).click();
  await page.keyboard.press('Escape');
  await dialog.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialog).toHaveCount(0);
}

/**
 * Cadastra um tenant novo pela UI (onboarding self-service), loga, cria um
 * servico, cadastra um profissional vinculado a ele e gera horarios para
 * amanha. Cada teste usa seu proprio tenant (slug/email com timestamp),
 * entao os testes nao interferem entre si e podem rodar quantas vezes forem
 * necessarias sem limpeza manual do banco.
 */
async function cadastrarTenantComServico(
  page: Page,
  opcoes: { slug: string; nomeServico: string; sinalPercentual: number },
) {
  const email = `${opcoes.slug}@e2e-teste.com`;
  const senha = 'senha12345';

  await page.goto('/auth/registrar');
  await page.getByLabel('Nome do negocio').fill(`Negocio ${opcoes.slug}`);
  await page.getByLabel('Link de agendamento (slug)').fill(opcoes.slug);
  await page.getByLabel('Abre as').fill('08:00');
  await page.getByLabel('Fecha as').fill('20:00');
  await page.getByLabel('Seu email (login do painel)').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(senha);
  await page.getByLabel('Confirmar senha').fill(senha);
  await page.getByRole('button', { name: 'Criar minha conta' }).click();

  await expect(page).toHaveURL(/\/auth\/login/);
  await expect(page.getByLabel('Email')).toHaveValue(email);
  await page.getByLabel('Senha').fill(senha);
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page).toHaveURL(/\/admin\/agenda/);

  await page.getByRole('tab', { name: 'Servicos' }).click();
  await page.getByRole('button', { name: 'Novo servico' }).click();
  const dialogServico = page.getByRole('dialog');
  await dialogServico.getByLabel('Nome').fill(opcoes.nomeServico);
  await dialogServico.getByLabel('Duracao (minutos)').fill('30');
  await dialogServico.getByLabel('Preco (R$)').fill('40');
  await dialogServico.getByLabel('Sinal (%)').fill(String(opcoes.sinalPercentual));
  await dialogServico.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogServico).toHaveCount(0);
  await expect(page.getByText(opcoes.nomeServico)).toBeVisible();

  await cadastrarProfissional(page, opcoes.nomeServico);

  await page.getByRole('tab', { name: 'Agenda' }).click();
  await page.getByRole('button', { name: 'Gerar horarios' }).click();
  const dialogSlots = page.getByRole('dialog');
  await dialogSlots.getByLabel('Profissional').click();
  await page.getByRole('option', { name: NOME_PROFISSIONAL }).click();
  await dialogSlots.getByLabel('Servico').click();
  await page.getByRole('option', { name: opcoes.nomeServico }).click();
  await dialogSlots.locator('input[type="date"]').fill(dataDeAmanha());
  await dialogSlots.getByRole('button', { name: 'Gerar' }).click();
  await expect(dialogSlots).toHaveCount(0);
}

test('reserva com servico sem sinal e confirmada na hora, sem cobranca Pix', async ({ page }) => {
  const slug = slugUnico('e2e-sem-sinal');
  await cadastrarTenantComServico(page, { slug, nomeServico: 'Corte Rapido', sinalPercentual: 0 });

  await page.goto(`/agendar/${slug}`);
  await page.getByText('Corte Rapido').click();
  await page.getByText(NOME_PROFISSIONAL).click();
  await page.locator('.campo-data').fill(dataDeAmanha());
  await page.locator('.item-horario').first().click();
  await page.getByLabel('Nome completo').fill('Cliente E2E');
  await page.getByLabel('Telefone (WhatsApp)').fill('+5511999998888');
  await page.getByLabel('CPF/CNPJ').fill(CPF_TESTE);
  await page.getByRole('button', { name: 'Confirmar e gerar Pix' }).click();

  await expect(page).toHaveURL(/\/agendar\/.+\/reserva\/\d+/);
  await expect(page.getByText('Reserva confirmada! O pagamento sera feito no local')).toBeVisible({
    timeout: 15_000,
  });
});

test('reserva com sinal exibe cobranca Pix real da Asaas aguardando pagamento', async ({ page }) => {
  const apiKey = lerChaveAsaasLocal();
  test.skip(!apiKey, 'Chave sandbox da Asaas nao encontrada em application-local.yml - pulando teste dependente dela.');

  const slug = slugUnico('e2e-com-sinal');
  await cadastrarTenantComServico(page, { slug, nomeServico: 'Corte Premium', sinalPercentual: 50 });
  await configurarChaveAsaas(page, apiKey!);

  await page.goto(`/agendar/${slug}`);
  await page.getByText('Corte Premium').click();
  await page.getByText(NOME_PROFISSIONAL).click();
  await page.locator('.campo-data').fill(dataDeAmanha());
  await page.locator('.item-horario').first().click();
  await page.getByLabel('Nome completo').fill('Cliente E2E');
  await page.getByLabel('Telefone (WhatsApp)').fill('+5511999998888');
  await page.getByLabel('CPF/CNPJ').fill(CPF_TESTE);
  await page.getByRole('button', { name: 'Confirmar e gerar Pix' }).click();

  await expect(page).toHaveURL(/\/agendar\/.+\/reserva\/\d+/, { timeout: 15_000 });
  await expect(page.locator('.pix-payload')).toContainText(/\S/, { timeout: 15_000 });
  await expect(page.getByText('Aguardando confirmacao do pagamento')).toBeVisible();
});
