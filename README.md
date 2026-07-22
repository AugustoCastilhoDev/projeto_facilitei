# Facilitei

[![CI](https://github.com/AugustoCastilhoDev/projeto_facilitei/actions/workflows/ci.yml/badge.svg)](https://github.com/AugustoCastilhoDev/projeto_facilitei/actions/workflows/ci.yml)

SaaS de agendamento com sinal via Pix para pequenos negócios de serviço (barbearias, salões, esteticistas etc.). Cada negócio (tenant) cadastra seus serviços, gera horários a partir do expediente configurado, e recebe agendamentos através de uma página pública própria — o cliente final escolhe serviço e horário, paga o sinal via Pix (Asaas) e a reserva é confirmada automaticamente por webhook.

Projeto construído como portfólio técnico, documentando as decisões de arquitetura e os problemas reais encontrados (e corrigidos) ao longo do desenvolvimento.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1.0, Spring Data JPA, Spring Security (JWT nativo via OAuth2 Resource Server), Flyway |
| Banco de dados | PostgreSQL 17 |
| Frontend | Angular 22 (standalone components, Signals), Angular Material 3 |
| Pagamento | Asaas (sandbox) — cobrança Pix |
| Notificação | Abstração própria (`NotificationService`), implementação atual é um mock de console — WhatsApp real fica para uma iteração futura |

As versões foram escolhidas pelas mais atuais estáveis no momento do desenvolvimento, não pelas mais populares/antigas — Spring Boot 4 e Angular 22 trouxeram mudanças de modularização (ver seção de decisões técnicas) que exigiram ajustes não documentados nos tutoriais mais comuns da internet.

## Arquitetura

### Backend — organizado por domínio, não por camada técnica

```
backend/src/main/java/com/castilhodigital/facilitei/
├── auth/          # JWT, login, registro de tenant+admin, TenantSecurityGuard (defesa IDOR)
├── tenant/         # Tenant (negócio), endpoints públicos de info do tenant
├── user/           # Usuário admin (role ADMIN)
├── catalog/        # Serviços oferecidos (ServiceOffering) — CRUD admin + listagem pública
├── scheduling/     # Slot, geração automática de horários, agenda admin/pública
├── booking/        # Reserva do cliente final, checkout, expiração automática
├── payment/        # Porta PaymentGatewayService + implementação payment/asaas
├── notification/   # Porta NotificationService + implementação console (mock)
└── common/         # Config transversal (segurança, exceptions, ProblemDetail, scheduling)
```

**Ports-and-adapters** para as duas integrações externas: `PaymentGatewayService` e `NotificationService` são interfaces; o domínio (`BookingCheckoutService`, `BookingService`) não sabe que o provedor de pagamento é a Asaas nem que a notificação por enquanto só loga no console. Trocar de gateway de pagamento ou plugar WhatsApp de verdade não exige mudar nada fora do respectivo pacote `payment.asaas` / `notification`.

### Frontend — feature-based, standalone components

```
frontend/src/app/
├── core/
│   ├── guards/         # authGuard (protege /admin)
│   ├── interceptors/   # authInterceptor (anexa Bearer só em chamadas /api/admin)
│   ├── models/         # Interfaces TypeScript espelhando os DTOs do backend
│   └── services/       # HttpClient wrappers (um por domínio: auth, service-offering, slot, public-booking)
└── features/
    ├── auth/            # Login
    ├── admin/           # Shell (Dashboard) + Agenda + Serviços (lazy-loaded)
    └── public-booking/  # Página pública de agendamento (lazy-loaded)
```

Todas as rotas usam `loadComponent`/`loadChildren` (lazy loading) e `withComponentInputBinding()` — parâmetros de rota (`:slug`, `:bookingId`) chegam direto como `input()` do componente, sem precisar ler `ActivatedRoute` manualmente.

### Multi-tenancy e segurança

- O `tenantId` fica no path da API (`/api/admin/tenants/{tenantId}/...`) por legibilidade, mas **nunca é confiado cegamente**: `TenantSecurityGuard` confere, em toda requisição admin, se o `tenantId` do path bate com o `tenantId` do JWT autenticado — fecha um IDOR clássico (OWASP API1:2023).
- Erros da API seguem o padrão RFC 7807 (`ProblemDetail`) de forma uniforme, inclusive para 401/403 (exigiu registrar o entry point tanto globalmente quanto no próprio `oauth2ResourceServer`, que tem um mecanismo interno que ignora o handler global para token malformado).
- Segredos (chave da Asaas) só existem em `application-local.yml`, que está no `.gitignore` — nunca chegam ao repositório nem foram colados em texto puro em nenhum lugar rastreado. O `application.yml` versionado já resolve tudo via variável de ambiente (`${ASAAS_API_KEY:}`, `${JWT_SECRET:...}`, `${DB_PASSWORD:...}`) — em produção basta configurar as variáveis no provedor de hospedagem, sem precisar de nenhum arquivo `.yml` adicional.
- `LoginRateLimiter` bloqueia força bruta no `/api/auth/login`: até 5 tentativas com falha por IP numa janela de 1 minuto; um login bem-sucedido zera o contador. Estado em memória (por instância) — suficiente para um único backend, mas exigiria um armazenamento compartilhado (Redis) se o deploy passar a ter múltiplas instâncias.

## Modelo de dados

```
tenants (negócio) ──< users (admin, role fixa)
        │
        ├──< services (nome, duração, preço, % sinal, ativo)
        │        │
        │        └──< slots (data/hora concreta, status: DISPONIVEL/RESERVADO/CONFIRMADO)
        │                 │
        │                 └── 1:1 bookings (cliente final, status pagamento, dados Pix)
```

- Um **slot** pertence a um único serviço e é gerado automaticamente (`SlotGenerationService`) a partir do expediente do tenant + duração do serviço.
- O negócio é modelado como **um único profissional/cadeira**: dois serviços diferentes não podem ter reservas em horários que se sobrepõem (ver decisão técnica abaixo).
- `bookings.status_pagamento` cobre `PENDENTE`, `PAGO`, `SEM_SINAL` (serviço sem sinal, pagamento no local), `EXPIRADO` e `CANCELADO`.

## Decisões técnicas

Pontos que valem a pena mencionar numa entrevista — cada um resolveu um problema real encontrado durante o desenvolvimento, não é só "boa prática" abstrata.

- **JWT nativo do Spring Security (OAuth2 Resource Server) em vez de uma lib como jjwt.** No momento do desenvolvimento, jjwt ainda não tinha compatibilidade confirmada com Jackson 3/Spring Framework 7 (trazidos pelo Boot 4.1) — o suporte nativo evita esse risco de dependência transitiva quebrada.
- **`open-in-view: false` + queries `JOIN FETCH` explícitas.** Descoberto via teste manual real: acessar `slot.getService().getNome()` fora da transação estourava `LazyInitializationException`. A solução não foi religar o `open-in-view` (esconde o problema, degrada performance em produção), mas escrever os repositórios com fetch explícito onde o dado é realmente necessário.
- **Conflito de horário entre serviços diferentes por sobreposição de intervalo, não por igualdade exata.** Como o negócio é uma cadeira/profissional só, reservar "Corte" (30min) não pode deixar "Coloração" (90min) disponível num horário que se sobreponha, mesmo que não comecem exatamente no mesmo minuto. A checagem roda em dois pontos: na listagem pública (esconde da vitrine) e na criação da reserva (bloqueio real, defesa contra corrida entre requisições concorrentes).
- **Expiração de reserva pendente: dois mecanismos complementares.** O webhook `PAYMENT_OVERDUE` da Asaas usa `dueDate`, que é só uma *data* — não tem precisão de horário. Uma reserva feita hoje às 14h para um horário daqui a pouco só venceria por lá "amanhã", muito depois do compromisso já ter passado. Por isso existe também um `@Scheduled` (`BookingExpirationScheduler`, a cada 5 minutos) que expira reservas pendentes cujo horário do slot esteja a 1h de distância ou menos — validado simulando o cenário real (booking com slot no passado, aguardando o scheduler rodar de fato).
- **QR Code Pix não é persistido, mas é re-buscável.** A imagem do QR Code não fica salva no banco (só o payload "copia e cola" e o id do pagamento), mas a página de pagamento sobrevive a um F5 graças à rota `/agendar/:slug/reserva/:bookingId` + um endpoint que rebusca o QR Code na Asaas enquanto o pagamento estiver pendente.
- **Sinal zero vira "pagamento no local", não uma cobrança de R$0,00.** A Asaas não aceita cobrança de valor zero. Se `sinalPercentual = 0`, o checkout pula a Asaas inteiramente e confirma a reserva direto (`status_pagamento = SEM_SINAL`).
- **Webhook da Asaas tem uma "fila de sincronização" própria, separada do cadastro da URL.** Ao testar com um túnel ngrok real, o webhook ficou marcado como "Interrompido" mesmo com a URL e o token corretos — o motivo não é falha de entrega (o log de webhooks da Asaas não registrava nenhuma tentativa), e sim um toggle "Fila de sincronização ativada?" que vem desligado por padrão ao criar o webhook. Eventos são gerados mesmo com a fila pausada, mas só são entregues depois que ela é reativada manualmente no painel.

## Limitações conhecidas

- **Sem split de pagamento**: o sinal cai na conta Asaas da própria plataforma; repassar ao dono do negócio seria manual (existe um campo `asaas_wallet_id` em `tenants` reservado para isso no futuro).
- **Um cliente não pode agendar múltiplos serviços numa única reserva/checkout** — decisão consciente de escopo, ver histórico do projeto. O modelo atual é 1 slot = 1 booking.
- **Webhook validado com túnel público (ngrok) contra a sandbox da Asaas**, incluindo uma reserva real, confirmação de pagamento pelo painel da Asaas e recebimento do evento `PAYMENT_CONFIRMED` pelo backend local — mas ainda não em produção, nem sob carga.
- **Fuso horário fixo em `America/Sao_Paulo`** — não há suporte a tenants em outros fusos.
- **CPF/CNPJ é obrigatório no formulário público mesmo quando o serviço não cobra sinal** (a validação está no DTO, não condicionada ao serviço escolhido) — só é estritamente necessário quando uma cobrança Pix de fato será gerada.

## Como rodar localmente

### Pré-requisitos
- Java 21
- Node 22+ com Angular CLI 22 (`npm i -g @angular/cli`)
- Docker (para o Postgres)
- Uma conta sandbox na [Asaas](https://sandbox.asaas.com) (grátis) — só necessária se você quiser testar a geração real de cobrança Pix

### 1. Subir o banco

```bash
docker compose up -d
```

### 2. Configurar a chave da Asaas (opcional, mas necessário para o fluxo de pagamento)

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
```

Edite o arquivo criado e cole sua chave da API sandbox da Asaas. Esse arquivo está no `.gitignore` — nunca será commitado.

### 3. Subir o backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

No PowerShell, envolva o parâmetro em aspas (senão ele é fatiado incorretamente):
```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

O Flyway aplica as migrations automaticamente na primeira subida.

### 4. Cadastrar um tenant de teste

Não existe tela de cadastro no painel — só a API:

```bash
curl -X POST http://localhost:8080/api/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{
    "nomeNegocio": "Barbearia Teste",
    "slug": "barbearia-teste",
    "horarioAbertura": "09:00:00",
    "horarioFechamento": "18:00:00",
    "emailAdmin": "admin@teste.com",
    "senhaAdmin": "senha12345"
  }'
```

### 5. Subir o frontend

```bash
cd frontend
npm install
npm start
```

- Painel admin: `http://localhost:4200/auth/login` (login com o email/senha cadastrados acima)
- Página pública de agendamento: `http://localhost:4200/agendar/barbearia-teste`

O dev server do Angular já tem um proxy configurado (`proxy.conf.json`) que encaminha `/api` para `http://localhost:8080` — não precisa configurar CORS.

## Testes

```bash
# Backend (JUnit 5 + Mockito + MockMvc)
cd backend && mvn test

# Frontend (Vitest)
cd frontend && ng test --watch=false
```

56 testes no backend e 9 suítes no frontend, cobrindo desde regras de negócio isoladas (cálculo de sinal, conflito de horário, expiração, rate limiting) até os controllers REST e a integração real com o sandbox da Asaas.

O GitHub Actions (`.github/workflows/ci.yml`) roda exatamente esses mesmos comandos — `mvn test` + `mvn package` no backend, `ng test` + `ng build` no frontend — a cada push e pull request para a `main`. Nenhum teste depende de banco de dados real (tudo via `@WebMvcTest`/Mockito ou testes puros de unidade), então o job do backend não precisa subir um Postgres.

Além desses, há uma suíte de testes end-to-end (Playwright) em [`e2e/`](e2e/README.md), que dirige a UI de verdade num navegador real: cadastro do negócio → login → criação de serviço → geração de horários → agendamento público, cobrindo tanto o fluxo sem sinal (confirmação imediata) quanto o fluxo com sinal (cobrança Pix real na sandbox da Asaas). Roda só localmente por enquanto — ver o README da pasta para pré-requisitos.
