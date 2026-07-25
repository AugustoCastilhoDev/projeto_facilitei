import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { Page, expect, test } from '@playwright/test';

const CPF_TESTE = '52998224725';
const NOME_PROFISSIONAL = 'Profissional Teste Portal';

function slugUnico(prefixo: string): string {
  return `${prefixo}-${Date.now()}`;
}

function dataDeAmanha(): string {
  const amanha = new Date();
  amanha.setDate(amanha.getDate() + 1);
  return amanha.toISOString().slice(0, 10);
}

/** DDD 63 + 9 digitos derivados do timestamp - unico por execucao (clientes.telefone e unique no backend). */
function telefoneUnico(): string {
  return `6399${Date.now().toString().slice(-7)}`;
}

/**
 * O provider de notificacao default (facilitei.notification.provider=console,
 * ver ConsoleOtpNotificationSender) so loga o codigo OTP - sem uma conta
 * MyZap real configurada (nao disponivel no CI), le-lo direto do log do
 * backend e o unico jeito de automatizar o login do portal ponta a ponta.
 * Poll curto porque a escrita do log pode atrasar alguns ms em relacao a
 * resposta HTTP do "solicitar-codigo".
 */
async function lerCodigoDoLog(telefoneNormalizado: string): Promise<string> {
  const caminhoLog = resolve(__dirname, '../../backend.log');
  const regex = new RegExp(`\\[CODIGO DE VERIFICACAO -> ${telefoneNormalizado}\\] (\\d{6})`, 'g');

  for (let tentativa = 0; tentativa < 40; tentativa++) {
    try {
      const conteudo = readFileSync(caminhoLog, 'utf8');
      const ocorrencias = [...conteudo.matchAll(regex)];
      if (ocorrencias.length > 0) {
        return ocorrencias[ocorrencias.length - 1][1];
      }
    } catch {
      // arquivo ainda pode nao existir se o backend acabou de subir - tenta de novo
    }
    await new Promise((resolveTimeout) => setTimeout(resolveTimeout, 250));
  }

  throw new Error(`Codigo de verificacao para ${telefoneNormalizado} nao encontrado em backend.log`);
}

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
 * Cadastra um negocio pela UI (mesmo onboarding self-service de
 * booking-flow.spec.ts), cria um servico sem sinal (evita depender da chave
 * sandbox da Asaas, indisponivel no CI - ver README de e2e), gera horarios
 * para amanha e opta por aparecer no diretorio publico do portal.
 */
async function cadastrarNegocioVisivelNoDiretorio(
  page: Page,
  opcoes: { slug: string; nomeServico: string; cidade: string },
): Promise<void> {
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
  await page.getByLabel('Senha').fill(senha);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/admin\/agenda/);

  await page.getByRole('tab', { name: 'Servicos' }).click();
  await page.getByRole('button', { name: 'Novo servico' }).click();
  const dialogServico = page.getByRole('dialog');
  await dialogServico.getByLabel('Nome').fill(opcoes.nomeServico);
  await dialogServico.getByLabel('Duracao (minutos)').fill('30');
  await dialogServico.getByLabel('Preco (R$)').fill('40');
  await dialogServico.getByLabel('Sinal (%)').fill('0');
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

  await page.getByRole('tab', { name: 'Diretorio' }).click();
  await page.getByLabel('Categoria').click();
  await page.getByRole('option', { name: 'Barbearia' }).click();
  await page.getByLabel('Cidade').fill(opcoes.cidade);
  await page.getByRole('switch', { name: 'Aparecer no diretorio publico' }).click();
  await page.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByText('Configuracao do diretorio salva.')).toBeVisible();
}

async function loginNoPortal(page: Page, telefone: string, nome: string): Promise<void> {
  await page.goto('/portal/login');
  await page.getByLabel('Telefone (com DDD)').fill(telefone);
  await page.getByRole('button', { name: 'Enviar codigo' }).click();

  await expect(page.getByLabel('Codigo recebido')).toBeVisible();
  const codigo = await lerCodigoDoLog(telefone);

  await page.getByLabel('Codigo recebido').fill(codigo);
  await page.getByLabel('Seu nome (so no primeiro acesso)').fill(nome);
  await page.getByRole('button', { name: 'Confirmar codigo' }).click();

  await expect(page).toHaveURL(/\/portal\/diretorio/);
}

test('cliente loga no portal via codigo, encontra o negocio no diretorio, agenda e ve a reserva no historico', async ({
  page,
}) => {
  const slug = slugUnico('e2e-portal');
  const cidade = `CidadeTeste${Date.now()}`;
  const nomeServico = 'Corte Portal';
  await cadastrarNegocioVisivelNoDiretorio(page, { slug, nomeServico, cidade });

  const telefoneCliente = telefoneUnico();
  await loginNoPortal(page, telefoneCliente, 'Cliente Portal E2E');

  // busca pela cidade unica do teste para nao pegar negocios de outras execucoes
  await page.getByLabel('Cidade').fill(cidade);
  await page.getByRole('button', { name: 'Buscar' }).click();
  await expect(page.getByText(`Negocio ${slug}`)).toBeVisible();

  await page.getByText(`Negocio ${slug}`).click();
  await expect(page).toHaveURL(new RegExp(`/portal/negocio/${slug}`));

  await page.getByText(nomeServico).click();
  await page.getByText(NOME_PROFISSIONAL).click();
  await page.locator('.campo-data').fill(dataDeAmanha());
  await page.locator('.item-horario').first().click();
  await page.getByLabel('CPF/CNPJ').fill(CPF_TESTE);
  await page.getByRole('button', { name: 'Confirmar e gerar Pix' }).click();

  await expect(page.getByText('Reserva confirmada! O pagamento sera feito no local')).toBeVisible({
    timeout: 15_000,
  });

  await page.getByRole('button', { name: 'Minhas reservas' }).click();
  await expect(page).toHaveURL(/\/portal\/minhas-reservas/);
  await expect(page.getByText(`Negocio ${slug}`)).toBeVisible();
  await expect(page.getByText('Confirmada (sem sinal)')).toBeVisible();
});
