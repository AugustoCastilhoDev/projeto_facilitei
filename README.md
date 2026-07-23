# Facilitei

[![CI](https://github.com/AugustoCastilhoDev/projeto_facilitei/actions/workflows/ci.yml/badge.svg)](https://github.com/AugustoCastilhoDev/projeto_facilitei/actions/workflows/ci.yml)

SaaS de agendamento com sinal via Pix para pequenos negócios de serviço (barbearias, salões, esteticistas etc.). Cada negócio (tenant) cadastra seus serviços e profissionais — cada profissional com seu próprio expediente e os serviços que realiza —, gera horários a partir dessa combinação, e recebe agendamentos através de uma página pública própria — o cliente final escolhe serviço, profissional e horário, paga o sinal via Pix (Asaas) e a reserva é confirmada automaticamente por webhook.

Projeto construído como portfólio técnico, documentando as decisões de arquitetura e os problemas reais encontrados (e corrigidos) ao longo do desenvolvimento.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1.0, Spring Data JPA, Spring Security (JWT nativo via OAuth2 Resource Server), Flyway |
| Banco de dados | PostgreSQL 17 |
| Frontend | Angular 22 (standalone components, Signals), Angular Material 3 |
| Pagamento | Asaas — cobrança Pix, modelo "traga sua própria conta" (BYOPP): cada tenant usa a própria chave de API |
| Notificação | Abstração própria (`NotificationService`) — WhatsApp real via [MyZap](https://www.myzap.net) (provedor não-oficial) ou mock de console, alternável por config |

As versões foram escolhidas pelas mais atuais estáveis no momento do desenvolvimento, não pelas mais populares/antigas — Spring Boot 4 e Angular 22 trouxeram mudanças de modularização (ver seção de decisões técnicas) que exigiram ajustes não documentados nos tutoriais mais comuns da internet.

## Arquitetura

### Backend — organizado por domínio, não por camada técnica

```
backend/src/main/java/com/castilhodigital/facilitei/
├── auth/          # JWT, login, registro de tenant+admin, TenantSecurityGuard (defesa IDOR)
├── tenant/         # Tenant (negócio), endpoints públicos de info do tenant
├── user/           # Usuário admin (role ADMIN)
├── catalog/        # Serviços oferecidos (ServiceOffering) — CRUD admin + listagem pública
├── professional/   # Profissional (expediente próprio, N:N com serviços) — CRUD admin + listagem pública
├── scheduling/     # Slot (por profissional), geração automática de horários, agenda admin/pública
├── booking/        # Reserva do cliente final, checkout, expiração automática
├── payment/        # Porta PaymentGatewayService + implementação payment/asaas
├── notification/   # Porta NotificationService + implementações console (mock) e notification/myzap (WhatsApp real)
├── report/         # Relatório básico (faturamento, taxa de não comparecimento, clientes recorrentes)
└── common/         # Config transversal (segurança, exceptions, ProblemDetail, scheduling, crypto)
```

**Ports-and-adapters** para as duas integrações externas: `PaymentGatewayService` e `NotificationService` são interfaces; o domínio (`BookingCheckoutService`, `BookingService`) não sabe que o provedor de pagamento é a Asaas nem qual implementação de notificação está ativa. Trocar de gateway de pagamento, ou entre o mock de console e o WhatsApp real via MyZap (`facilitei.notification.provider=console|myzap`), não exige mudar nada fora do respectivo pacote `payment.asaas` / `notification`.

### Frontend — feature-based, standalone components

```
frontend/src/app/
├── core/
│   ├── guards/         # authGuard (protege /admin)
│   ├── interceptors/   # authInterceptor (anexa Bearer só em chamadas /api/admin)
│   ├── models/         # Interfaces TypeScript espelhando os DTOs do backend
│   └── services/       # HttpClient wrappers (um por domínio: auth, service-offering, slot, public-booking)
└── features/
    ├── auth/            # Login + registro (onboarding self-service)
    ├── admin/           # Shell (Dashboard) + Agenda + Serviços + Profissionais + Pagamentos (lazy-loaded)
    └── public-booking/  # Página pública de agendamento (lazy-loaded)
```

Todas as rotas usam `loadComponent`/`loadChildren` (lazy loading) e `withComponentInputBinding()` — parâmetros de rota (`:slug`, `:bookingId`) chegam direto como `input()` do componente, sem precisar ler `ActivatedRoute` manualmente.

### Multi-tenancy e segurança

- O `tenantId` fica no path da API (`/api/admin/tenants/{tenantId}/...`) por legibilidade, mas **nunca é confiado cegamente**: `TenantSecurityGuard` confere, em toda requisição admin, se o `tenantId` do path bate com o `tenantId` do JWT autenticado — fecha um IDOR clássico (OWASP API1:2023).
- Erros da API seguem o padrão RFC 7807 (`ProblemDetail`) de forma uniforme, inclusive para 401/403 (exigiu registrar o entry point tanto globalmente quanto no próprio `oauth2ResourceServer`, que tem um mecanismo interno que ignora o handler global para token malformado).
- Segredos de configuração da própria plataforma (JWT, banco) só existem em variável de ambiente/`application-local.yml` (git-ignorado) — nunca chegam ao repositório. Já as credenciais Asaas de cada **tenant** (modelo BYOPP, ver abaixo) ficam no banco, cifradas em repouso via `EncryptedStringConverter` (AES via `TextEncryptor` do Spring Security Crypto) — nunca em texto puro, nem sequer devolvidas pela API depois de salvas.
- `LoginRateLimiter` bloqueia força bruta no `/api/auth/login`: até 5 tentativas com falha por IP numa janela de 1 minuto; um login bem-sucedido zera o contador. Estado em memória (por instância) — suficiente para um único backend, mas exigiria um armazenamento compartilhado (Redis) se o deploy passar a ter múltiplas instâncias.

### Recebimento de pagamentos — modelo BYOPP ("traga sua própria conta")

A plataforma nunca guarda nem intermedia o dinheiro do sinal: cada tenant configura a **própria** chave de API Asaas (`TenantAsaasConfigController`, tela "Pagamentos" no painel), e a cobrança Pix é criada direto na conta Asaas do negócio — a chave é passada por chamada ao `AsaasClient` (não é mais um header fixo num `RestClient` global), resolvida a partir do tenant dono do agendamento.

Como cada tenant tem sua própria conta Asaas, o webhook (`AsaasWebhookController`) não pode mais validar contra um segredo único e global: ele primeiro descobre de qual tenant é o evento — buscando a reserva pelo `asaasPaymentId` recebido (sempre globalmente único na Asaas) — e só então compara o token recebido contra o `asaasWebhookToken` daquele tenant especificamente, gerado pela própria plataforma na primeira configuração.

Ver o tutorial completo (para o dono do negócio) em [`docs/configurar-pagamentos.md`](docs/configurar-pagamentos.md).

## Modelo de dados

```
tenants (negócio) ──< users (admin, role fixa)
        │
        ├──< services (nome, duração, preço, % sinal, ativo) ──┐
        │                                                      │
        ├──< profissionais (nome, expediente próprio, ativo) ──┤ N:N (profissional_servicos)
        │        │                                             │
        │        └──< slots (data/hora, serviço, status) ──────┘
        │                 │
        │                 └── 1:1 bookings (cliente final, status pagamento, dados Pix)
```

- Um **slot** pertence a um profissional e a um serviço, e é gerado automaticamente (`SlotGenerationService`) a partir do expediente do profissional + duração do serviço escolhido.
- Cada **profissional** tem seu próprio expediente (horário de abertura/fechamento) e uma lista própria de serviços que realiza (`profissional_servicos`) — dois negócios diferentes podem ter profissionais com turnos completamente distintos. Dois serviços do **mesmo** profissional não podem ter reservas em horários que se sobrepõem; profissionais diferentes têm agendas independentes (ver decisão técnica abaixo).
- `bookings.status_pagamento` cobre `PENDENTE`, `PAGO`, `SEM_SINAL` (serviço sem sinal, pagamento no local), `EXPIRADO` e `CANCELADO`.
- `tenants` guarda `asaas_api_key` e `asaas_webhook_token` (ambos cifrados) — a própria credencial Asaas do tenant, não da plataforma. Ver "Recebimento de pagamentos (BYOPP)" abaixo.

## Decisões técnicas

Pontos que valem a pena mencionar numa entrevista — cada um resolveu um problema real encontrado durante o desenvolvimento, não é só "boa prática" abstrata.

- **JWT nativo do Spring Security (OAuth2 Resource Server) em vez de uma lib como jjwt.** No momento do desenvolvimento, jjwt ainda não tinha compatibilidade confirmada com Jackson 3/Spring Framework 7 (trazidos pelo Boot 4.1) — o suporte nativo evita esse risco de dependência transitiva quebrada.
- **`open-in-view: false` + queries `JOIN FETCH` explícitas.** Descoberto via teste manual real: acessar `slot.getService().getNome()` fora da transação estourava `LazyInitializationException`. A solução não foi religar o `open-in-view` (esconde o problema, degrada performance em produção), mas escrever os repositórios com fetch explícito onde o dado é realmente necessário.
- **Conflito de horário por sobreposição de intervalo, não por igualdade exata, escopado por profissional.** Reservar "Corte" (30min) não pode deixar "Coloração" (90min) disponível num horário que se sobreponha, mesmo que não comecem exatamente no mesmo minuto — mas só quando é o **mesmo profissional**: como cada `Slot` carrega uma FK própria para `Profissional`, a query de sobreposição (`findOcupadosNoIntervalo`) filtra por `profissional.id`, não mais por `tenant.id` como na versão de "profissional único". A checagem roda em dois pontos: na listagem pública (esconde da vitrine) e na criação da reserva (bloqueio real, defesa contra corrida entre requisições concorrentes).
- **Serviço vinculado a profissionais específicos (N:N), não "qualquer profissional faz qualquer serviço".** Decisão consciente para refletir o caso real (nem todo profissional de uma barbearia faz coloração, por exemplo) — custou uma tabela de junção (`profissional_servicos`) e um filtro a mais no fluxo público (`GET /public/tenants/{slug}/profissionais?serviceId=`), mas evita a UX confusa de oferecer um profissional que não presta aquele serviço.
- **Expiração de reserva pendente: dois mecanismos complementares.** O webhook `PAYMENT_OVERDUE` da Asaas usa `dueDate`, que é só uma *data* — não tem precisão de horário. Uma reserva feita hoje às 14h para um horário daqui a pouco só venceria por lá "amanhã", muito depois do compromisso já ter passado. Por isso existe também um `@Scheduled` (`BookingExpirationScheduler`, a cada 5 minutos) que expira reservas pendentes cujo horário do slot esteja a 1h de distância ou menos — validado simulando o cenário real (booking com slot no passado, aguardando o scheduler rodar de fato).
- **QR Code Pix não é persistido, mas é re-buscável.** A imagem do QR Code não fica salva no banco (só o payload "copia e cola" e o id do pagamento), mas a página de pagamento sobrevive a um F5 graças à rota `/agendar/:slug/reserva/:bookingId` + um endpoint que rebusca o QR Code na Asaas enquanto o pagamento estiver pendente.
- **Sinal zero vira "pagamento no local", não uma cobrança de R$0,00.** A Asaas não aceita cobrança de valor zero. Se `sinalPercentual = 0`, o checkout pula a Asaas inteiramente e confirma a reserva direto (`status_pagamento = SEM_SINAL`).
- **Webhook da Asaas tem uma "fila de sincronização" própria, separada do cadastro da URL.** Ao testar com um túnel ngrok real, o webhook ficou marcado como "Interrompido" mesmo com a URL e o token corretos — o motivo não é falha de entrega (o log de webhooks da Asaas não registrava nenhuma tentativa), e sim um toggle "Fila de sincronização ativada?" que vem desligado por padrão ao criar o webhook. Eventos são gerados mesmo com a fila pausada, mas só são entregues depois que ela é reativada manualmente no painel.
- **Chave Asaas por tenant, cifrada via `AttributeConverter`, não manualmente antes de salvar.** `EncryptedStringConverter` cifra/decifra de forma transparente no próprio mapeamento JPA (`@Convert`) — a entidade sempre trabalha com o texto plano em memória, só a coluna no banco fica cifrada. O conversor é registrado como bean Spring (`@Component`), não instanciado via reflection pelo Hibernate, para poder injetar o `TextEncryptor` — o Spring Boot já configura o Hibernate para resolver conversores assim.
- **Identificar o tenant pelo pagamento antes de autenticar o webhook, não depois.** Com uma chave/token só por conta Asaas (modelo BYOPP), não existe mais um segredo único global para comparar. A solução inverte a ordem: busca a reserva pelo `asaasPaymentId` (globalmente único), pega o tenant dono dela, e só então valida o token contra o segredo daquele tenant específico — descoberto durante a implementação que essa consulta precisa de `JOIN FETCH` explícito (`slot` + `tenant`), porque o controller acessa o tenant fora da transação onde a reserva foi buscada (`LazyInitializationException` real, pego em teste manual, não só em teoria).
- **O mesmo bug de `LazyInitializationException` reapareceu numa coleção `@ManyToMany` (`Profissional.servicos`).** Ao expor `ProfissionalResponse.from()` (que lê `profissional.getServicos()`) fora da transação, o padrão de fetch explícito precisou ser aplicado de novo — mas dessa vez com um cuidado a mais: os métodos que **filtram** por serviço (`findByTenantIdAndServicosIdAndAtivoTrueOrderByNome`) não podem usar o mesmo `JOIN` tanto para o filtro quanto para o `FETCH` da coleção, senão a coleção carregada fica incompleta (só o item que bateu no filtro). A solução usa um `LEFT JOIN FETCH` sem condição para trazer a coleção inteira, e um `EXISTS` subquery separado só para o filtro. Nenhum dos 4 testes automatizados do `ProfissionalService`/controllers pegou isso (mockam o repositório) — só apareceu ao testar manualmente o CRUD de profissionais na UI, reforçando por que essa etapa nunca é pulada neste projeto.
- **Falha no envio de WhatsApp nunca pode derrubar a reserva.** `BookingService.criarReserva`/`confirmarPagamento`/etc. chamam `NotificationService.enviar()` dentro do mesmo `@Transactional` que grava o booking/slot — se a exceção subisse, uma instabilidade momentânea do MyZap faria a reserva inteira sofrer rollback por causa de um efeito colateral (notificar o cliente), não da regra de negócio em si. `MyZapNotificationService` captura qualquer falha do `MyZapClient` e só loga (best effort), nunca relança.
- **Chave do MyZap é única da plataforma, não por tenant (diferente do modelo BYOPP da Asaas).** O envio de WhatsApp sai de uma única conta/número da Facilitei — não há (ainda) isolamento por tenant nessa integração, então a chave é um header fixo do `RestClient` (`MyZapConfig`), configurada uma vez via `facilitei.myzap.api-key`, ao contrário do `access_token` da Asaas que é passado por chamada.
- **`CANCELADO` existia no enum `PaymentStatus` sem nenhum código usá-lo.** Ao implementar os relatórios, uma "taxa de não comparecimento" real exigia primeiro um jeito de o admin registrar o que de fato aconteceu com a reserva — isso motivou ativar o cancelamento manual (`BookingService.cancelar`) e adicionar a marcação de comparecimento (`Booking.compareceu`), ambos expostos na tela Agenda.
- **Relatório calculado em memória com Streams, não com uma query SQL `GROUP BY`.** É a primeira agregação do projeto — o volume de reservas confirmadas por tenant/mês é pequeno o bastante (dezenas a poucas centenas) para não justificar a complexidade extra de uma projeção JPQL agregada; um `JOIN FETCH` simples + `Collectors.groupingBy` é mais legível e fácil de testar.
- **`SlotResponse` ganhou dados de `Booking` sem criar uma associação bidirecional `Slot ↔ Booking`.** Isso criaria acoplamento de entidade cíclico entre os pacotes `scheduling`/`booking` só para exibir cliente/status na agenda do admin. Em vez disso, `SlotAdminController.listarAgenda` busca os slots e os bookings correspondentes separadamente (`BookingService.buscarPorSlotIds`) e faz o merge no próprio controller — camada que já atua como ponto de composição entre pacotes neste projeto (mesmo espírito de `BookingCheckoutService`).

## Limitações conhecidas

- **Sem onboarding assistido da conta Asaas do tenant**: o dono do negócio (ou a equipe da Facilitei, ajudando-o) precisa criar a própria conta Asaas e colar a chave manualmente — não há criação automática de subconta nem fluxo OAuth/Connect. Existe um campo `asaas_wallet_id` em `tenants`, ainda não utilizado, reservado para um modelo de split via marketplace avaliado e descartado em favor do BYOPP (ver ROADMAP).
- **Um único webhook token por tenant, sem rotação**: gerado uma vez na primeira configuração e nunca trocado (mesmo ao atualizar a chave de API) — evita invalidar um webhook já configurado do lado da Asaas, mas também não há como o próprio tenant regenerá-lo pela UI se suspeitar de vazamento.
- **Um cliente não pode agendar múltiplos serviços numa única reserva/checkout** — decisão consciente de escopo, ver histórico do projeto. O modelo atual é 1 slot = 1 booking.
- **Expediente do profissional é um único intervalo fixo por dia** (abre/fecha), sem variação por dia da semana nem controle de folgas/férias — gerar horários para um dia específico continua sendo uma ação manual do admin (tela Agenda).
- **Vincular serviços a um profissional só é possível pela tela "Profissionais"**, não há atalho pela tela "Serviços" para ver/editar quais profissionais realizam aquele serviço.
- **Webhook validado com túnel público (ngrok) contra a sandbox da Asaas**, incluindo uma reserva real, confirmação de pagamento pelo painel da Asaas e recebimento do evento `PAYMENT_CONFIRMED` pelo backend local — mas ainda não em produção, nem sob carga.
- **Fuso horário fixo em `America/Sao_Paulo`** — não há suporte a tenants em outros fusos.
- **CPF/CNPJ é obrigatório no formulário público mesmo quando o serviço não cobra sinal** (a validação está no DTO, não condicionada ao serviço escolhido) — só é estritamente necessário quando uma cobrança Pix de fato será gerada.
- **WhatsApp via MyZap é um provedor não-oficial** (automação sobre o WhatsApp Web/multi-dispositivo, não a Cloud API da Meta) — mais simples de configurar (sem verificação de empresa nem aprovação de template), mas o número usado corre o risco de ser suspenso pela Meta a qualquer momento, por estar fora dos termos de uso oficiais do WhatsApp.
- **Notificação por WhatsApp sai de uma única conta/número da plataforma**, não por tenant — todo negócio que usa o Facilitei hoje compartilha o mesmo remetente.
- **Faturamento do relatório não distingue sinal já recebido do valor total combinado a receber no local.** Conta o preço cheio do serviço para toda reserva confirmada que não foi marcada como não comparecimento, mas o sistema só sabe com certeza que o *sinal* (quando há) foi pago via Pix — o restante é um valor combinado presencialmente, sem registro.
- **Comparecimento é 100% manual** — não há lembrete automático nem confirmação do próprio cliente; se o admin nunca marcar, a reserva simplesmente não entra no cálculo da taxa de não comparecimento (fica de fora do denominador, não conta como comparecimento).

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

### 2. Chave da Asaas (opcional, só necessária para testar cobrança real de Pix)

Desde o modelo BYOPP ("traga sua própria conta"), a chave usada para gerar a cobrança Pix do sinal **não** vem mais de `application-local.yml` — cada tenant configura a própria, direto no painel (aba "Pagamentos", depois de logar). `application-local.yml` continua existindo só para a chave *da própria plataforma*, reservada para uma cobrança futura ainda não implementada (a assinatura do SaaS):

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
```

Esse arquivo está no `.gitignore` — nunca será commitado. Para testar a geração real de um Pix pelo tenant, use uma chave sandbox da Asaas na tela "Pagamentos" do painel (ver [`docs/configurar-pagamentos.md`](docs/configurar-pagamentos.md)).

O mesmo `application-local.yml` também tem a chave do **MyZap** (WhatsApp), com `facilitei.notification.provider` como `console` (default, só loga) ou `myzap` (envia de verdade). Sem uma conta MyZap configurada, deixe em `console`.

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

Depois de subir o frontend (próximo passo), acesse `http://localhost:4200/auth/registrar` e cadastre pelo formulário (onboarding self-service). Se preferir via API diretamente:

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

O registro cria automaticamente um profissional padrão ("Profissional 1") com o expediente informado no formulário, mas sem nenhum serviço vinculado ainda — depois de cadastrar um serviço na aba "Serviços", vincule-o a esse profissional (ou crie outros) na aba "Profissionais" antes de gerar horários.

O dev server do Angular já tem um proxy configurado (`proxy.conf.json`) que encaminha `/api` para `http://localhost:8080` — não precisa configurar CORS.

## Testes

```bash
# Backend (JUnit 5 + Mockito + MockMvc)
cd backend && mvn test

# Frontend (Vitest)
cd frontend && ng test --watch=false
```

104 testes no backend e 13 suítes no frontend, cobrindo desde regras de negócio isoladas (cálculo de sinal, conflito de horário por profissional, expiração, rate limiting, criptografia de credenciais) até os controllers REST, a integração real com o sandbox da Asaas, o envio de WhatsApp via MyZap e o relatório básico.

O GitHub Actions (`.github/workflows/ci.yml`) roda exatamente esses mesmos comandos — `mvn test` + `mvn package` no backend, `ng test` + `ng build` no frontend — a cada push e pull request para a `main`. Nenhum teste depende de banco de dados real (tudo via `@WebMvcTest`/Mockito ou testes puros de unidade), então o job do backend não precisa subir um Postgres.

Além desses, há uma suíte de testes end-to-end (Playwright) em [`e2e/`](e2e/README.md), que dirige a UI de verdade num navegador real: cadastro do negócio → login → criação de serviço → geração de horários → agendamento público, cobrindo tanto o fluxo sem sinal (confirmação imediata) quanto o fluxo com sinal (cobrança Pix real na sandbox da Asaas). Roda só localmente por enquanto — ver o README da pasta para pré-requisitos.
