# E2E — Facilitei

Testes de ponta a ponta com [Playwright](https://playwright.dev), cobrindo o fluxo completo pela UI: cadastro do negócio (onboarding self-service) → login → criação de serviço → cadastro de profissional vinculado ao serviço → geração de horários → agendamento público (escolha de serviço, profissional e horário) → confirmação.

Cada teste cadastra o **seu próprio tenant** (slug e email com timestamp), então os testes não interferem entre si e podem ser rodados repetidamente sem nenhuma limpeza manual do banco.

## Pré-requisitos

Diferente dos testes unitários (`backend/mvn test`, `frontend/ng test`), a suíte E2E roda contra a aplicação real, então antes de executar:

1. **Postgres no ar**: `docker compose up -d` (na raiz do projeto)
2. **Backend rodando** na porta 8080, com o perfil `local` ativo (precisa da chave real da Asaas sandbox configurada em `application-local.yml` — ver README principal). Sem isso, o teste "com sinal" falha ao tentar gerar a cobrança Pix.
3. O frontend **não precisa** estar rodando manualmente — o `playwright.config.ts` sobe `npm start` automaticamente (`webServer`) e reaproveita o dev server se já estiver de pé.

## Rodando

```bash
cd e2e
npm install
npx playwright install chromium   # só na primeira vez
npm test
```

## O que é testado

- **Reserva sem sinal** (`sinalPercentual = 0`): a reserva é confirmada na hora, sem gerar cobrança Pix (fluxo `SEM_SINAL`).
- **Reserva com sinal**: gera uma cobrança Pix real na sandbox da Asaas e verifica que o QR Code/código copia-e-cola aparece na tela, aguardando pagamento.
- **Comparecimento e cancelamento manual + relatório**: marca uma reserva como "não compareceu" e cancela outra pela tela Agenda, depois confere que o relatório do período reflete os números corretos (faturamento, taxa de não comparecimento).

Os arquivos de spec rodam com **1 worker** (`playwright.config.ts`), não em paralelo entre si: como todos compartilham o mesmo backend/Postgres reais, rodar arquivos diferentes concorrentemente (2+ Chromium disputando CPU) já causou flakiness em interações de formulário — descoberto ao adicionar o segundo spec da suíte.

## Limitações conhecidas

- Não roda no CI (GitHub Actions) ainda — exigiria subir Postgres, aplicar migrations e iniciar o backend real dentro do workflow. Ver `ROADMAP.md`.
- Não simula a confirmação do pagamento em si (isso exigiria um Pix real ou o mesmo túnel ngrok usado para testar o webhook manualmente).
- Cada execução deixa um tenant novo no banco (não há endpoint de exclusão de tenant ainda) — aceitável para uso local, mas cresce indefinidamente se rodado muitas vezes.
