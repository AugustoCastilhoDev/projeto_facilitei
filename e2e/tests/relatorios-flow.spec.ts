import { expect, test } from '@playwright/test';

const CPF_TESTE = '52998224725';
const NOME_PROFISSIONAL = 'Profissional Teste';

function slugUnico(prefixo: string): string {
  return `${prefixo}-${Date.now()}`;
}

function dataDeAmanha(): string {
  const amanha = new Date();
  amanha.setDate(amanha.getDate() + 1);
  return amanha.toISOString().slice(0, 10);
}

/**
 * Mesmo helper de booking-flow.spec.ts (cadastro self-service + servico +
 * profissional + horarios para amanha), reescrito aqui para manter os dois
 * specs independentes um do outro sem import cruzado.
 */
async function cadastrarTenantComServico(
  page: import('@playwright/test').Page,
  opcoes: { slug: string; nomeServico: string },
) {
  const email = `${opcoes.slug}@e2e-teste.com`;
  const senha = 'senha12345';

  await page.goto('/auth/registrar');
  await page.getByLabel('Nome do negocio').fill(`Negocio ${opcoes.slug}`);
  await page.getByLabel('Link de agendamento (slug)').fill(opcoes.slug);
  await page.getByLabel('Abre as').fill('08:00');
  await page.getByLabel('Fecha as').fill('20:00');
  await page.getByLabel('CPF/CNPJ').fill(CPF_TESTE);
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
  await dialogServico.getByLabel('Preco (R$)').fill('100');
  await dialogServico.getByLabel('Sinal (%)').fill('0');
  await dialogServico.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogServico).toHaveCount(0);
  await expect(page.getByText(opcoes.nomeServico)).toBeVisible();

  await page.getByRole('tab', { name: 'Profissionais' }).click();
  await page.getByRole('button', { name: 'Novo profissional' }).click();
  const dialogProfissional = page.getByRole('dialog');
  await dialogProfissional.getByLabel('Nome').fill(NOME_PROFISSIONAL);
  await dialogProfissional.getByLabel('Abre as').fill('08:00');
  await dialogProfissional.getByLabel('Fecha as').fill('20:00');
  await dialogProfissional.getByLabel('Servicos que realiza').click();
  await page.getByRole('option', { name: opcoes.nomeServico }).click();
  await page.keyboard.press('Escape');
  await dialogProfissional.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialogProfissional).toHaveCount(0);

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

test('marcar reserva como nao compareceu reflete no relatorio e o cancelamento libera o horario', async ({ page }) => {
  const slug = slugUnico('e2e-relatorios');
  const nomeServico = 'Corte Relatorio';
  await cadastrarTenantComServico(page, { slug, nomeServico });

  // Reserva 1 (sera marcada como nao compareceu) via fluxo publico, sem sinal (confirma na hora).
  await page.goto(`/agendar/${slug}`);
  await page.getByText(nomeServico).click();
  await page.getByText(NOME_PROFISSIONAL).click();
  await page.locator('.campo-data').fill(dataDeAmanha());
  await page.locator('.item-horario').first().click();
  await page.getByLabel('Nome completo').fill('Cliente Faltante');
  await page.getByLabel('Telefone (WhatsApp)').fill('+5511999998888');
  await page.getByLabel('CPF/CNPJ').fill(CPF_TESTE);
  await page.getByRole('button', { name: 'Confirmar e gerar Pix' }).click();
  await expect(page.getByText('Reserva confirmada! O pagamento sera feito no local')).toBeVisible({
    timeout: 15_000,
  });

  // Reserva 2 (sera cancelada pelo admin) - horario seguinte.
  await page.goto(`/agendar/${slug}`);
  await page.getByText(nomeServico).click();
  await page.getByText(NOME_PROFISSIONAL).click();
  await page.locator('.campo-data').fill(dataDeAmanha());
  await page.locator('.item-horario').nth(1).click();
  await page.getByLabel('Nome completo').fill('Cliente Cancelado');
  await page.getByLabel('Telefone (WhatsApp)').fill('+5511999997777');
  await page.getByLabel('CPF/CNPJ').fill(CPF_TESTE);
  await page.getByRole('button', { name: 'Confirmar e gerar Pix' }).click();
  await expect(page.getByText('Reserva confirmada! O pagamento sera feito no local')).toBeVisible({
    timeout: 15_000,
  });

  // Volta pro painel admin (o token continua no localStorage da mesma sessao de navegador).
  await page.goto('/admin/agenda');
  await page.locator('.campo-data').fill(dataDeAmanha());
  await expect(page.getByText('Cliente Faltante')).toBeVisible();

  const linhaFaltante = page.locator('tr', { hasText: 'Cliente Faltante' });
  await linhaFaltante.getByRole('button', { name: 'Nao compareceu' }).click();
  await expect(page.getByText('Marcado como nao compareceu.')).toBeVisible();

  const linhaCancelada = page.locator('tr', { hasText: 'Cliente Cancelado' });
  page.once('dialog', (dialog) => dialog.accept());
  await linhaCancelada.getByRole('button', { name: 'Cancelar reserva' }).click();
  await expect(page.getByText('Reserva cancelada.')).toBeVisible();

  // Relatorio: periodo precisa cobrir "amanha" (default do formulario vai so ate hoje).
  await page.getByRole('tab', { name: 'Relatorios' }).click();
  await page.getByLabel('Ate').fill(dataDeAmanha());

  await expect(page.getByText('R$ 0.00')).toBeVisible();
  await expect(page.getByText('100.0%')).toBeVisible();
  await expect(page.getByText('1 de 1 marcados')).toBeVisible();
});
