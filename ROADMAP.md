# Roadmap — Facilitei: de MVP a produto vendável

> Como usar: marque `[x]` conforme for concluindo cada item. Sinta-se livre para editar, reordenar, adicionar notas/datas ou remover itens que deixarem de fazer sentido — este documento é vivo, não um contrato fechado.

O que existe hoje é uma base técnica sólida (agendamento multi-tenant com múltiplos profissionais por negócio, vínculo serviço↔profissional editável dos dois lados, sinal via Pix, WhatsApp real via MyZap, relatórios básicos, cobrança automática da própria assinatura SaaS com planos e trial, observabilidade básica (correlation id, Sentry, health check), painel admin, onboarding self-service, ~152 testes automatizados). O sinal já cai direto na conta Asaas de cada negócio (modelo "traga sua própria conta de pagamento" — BYOPP, ver README) — decisão tomada no lugar do split/marketplace originalmente cogitado, porque não exige formalização prévia com a Asaas nem custodia dinheiro de terceiros. A mensalidade da Facilitei em si (diferente do sinal) usa a chave Asaas da própria plataforma. Uma minuta de Termos de Uso/Política de Privacidade já foi escrita (`docs/`), pendente de revisão profissional. A hospedagem em produção já está decidida (VPS na Hostinger, datacenter em Boston/EUA, domínio via Registro.br) — a minuta de Política de Privacidade já reflete isso (transferência internacional de dados, seção 5). O que ainda falta para vender de verdade não é código — é fechar esse jurídico com um profissional e efetivamente subir o ambiente de produção nessa VPS.

Ver também: [README.md](README.md) (arquitetura técnica e como rodar o projeto).

---

## Onde estamos vs. onde precisa chegar

| Hoje (MVP) | Produto vendável |
|---|---|
| ~~Cadastro de negócio só via API (curl)~~ Onboarding self-service ✅ | — |
| ~~1 profissional/cadeira por negócio~~ Múltiplos profissionais, cada um com sua agenda ✅ | — |
| ~~Vincular serviço a profissional só pela tela de Profissionais~~ Editável também pela tela de Serviços ✅ | — |
| ~~Sinal cai na conta Asaas da plataforma~~ Sinal direto na conta do negócio (BYOPP) ✅ | Onboarding assistido/automatizado da conta Asaas do tenant (hoje é manual, copiar/colar a chave) |
| ~~Sem cobrança pela assinatura do SaaS~~ Cobrança automática via Pix, planos e trial de 30 dias ✅ | Troca de plano e reativação de assinatura cancelada pela própria UI (hoje é manual, contato com suporte) |
| Roda só localmente (docker compose) | Deploy em produção, com monitoramento |
| ~~Sem Termos de Uso nem Política de Privacidade~~ Minuta escrita, pendente revisão profissional ✅ | LGPD tratada, contrato entre plataforma e negócio |
| ~~Notificação = log no console~~ WhatsApp real via MyZap (não-oficial) ✅ | Avaliar migrar para a API oficial da Meta se o número usado for suspenso |

---

## Fase 0 — Validar com o que existe *(contínuo)*

- [ ] Conseguir 5–10 negócios reais (barbearias/salões pequenos) usando o MVP atual, de graça ou com desconto
- [x] ~~Cobrar a mensalidade combinada manualmente, fora do sistema, enquanto o billing próprio não existe~~ — billing próprio implementado (ver Fase 2), a cobrança já é automática
- [ ] Coletar feedback de uso real antes de investir pesado nas próximas fases

## Fase 1 — Fechar os bloqueadores *(~4–6 semanas)*

### Produto
- [x] Onboarding self-service — formulário público de cadastro de negócio (hoje só existe via curl)

### Infraestrutura
- [ ] Deploy em produção: Postgres gerenciado com backup, domínio, HTTPS — hospedagem já decidida (VPS Hostinger, Boston/EUA, mesma VPS de outros 2 projetos já em produção; domínio via Registro.br), falta subir o ambiente de fato
- [x] Testar o webhook da Asaas com URL pública de verdade (hoje só foi simulado localmente)
- [x] Hardening básico: rate limiting no login, segredos fora de arquivo `.yml` (usar secrets manager do provedor de hospedagem)

### Jurídico & financeiro
- [ ] Conversar com contador sobre CNPJ e enquadramento tributário correto
- [x] Escrever uma minuta de Termos de Uso + Política de Privacidade (LGPD) — deixa claro que a Facilitei não custodia o pagamento do sinal (modelo BYOPP), só a assinatura do SaaS; ver [`docs/termos-de-uso.md`](docs/termos-de-uso.md) e [`docs/politica-de-privacidade.md`](docs/politica-de-privacidade.md)
- [ ] Revisar a minuta acima com contador/advogado e preencher os campos pendentes (razão social, CNPJ, endereço, foro, e-mail de contato) antes de publicar de verdade — pontos de conteúdo já resolvidos com profissional: ausência de intermediação financeira (BYOPP vs. ITP), direito de arrependimento (art. 49 CDC), base legal por finalidade (art. 7º LGPD), DPO simplificado (Resolução CD/ANPD nº 2/2022) e transferência internacional de dados (Resolução CD/ANPD nº 19/2024); tabela de planos da seção 6 dos Termos de Uso já preenchida com os valores reais do enum `Plano`
- ~~Formalizar com a Asaas o modelo de "plataforma com split de pagamento"~~ — decidido não seguir por esse caminho: o modelo BYOPP não exige formalização nenhuma com a Asaas
- [x] Implementar o recebimento direto na conta do próprio tenant (modelo BYOPP: chave Asaas por tenant, cifrada em repouso, webhook autenticado por tenant) — ver README/`docs/configurar-pagamentos.md`

### Go-to-market
- [ ] Nada de aquisição paga ainda — só continuar validando com early adopters da Fase 0

## Fase 2 — Produto completo *(~2–3 meses)*

### Produto
- [x] Suporte a múltiplos profissionais/recursos por negócio — cada profissional com expediente próprio e seus próprios serviços vinculados (N:N); conflito de horário passou a ser escopado por profissional, não mais por tenant inteiro
- [x] WhatsApp real substituindo o mock de console — via [MyZap](https://www.myzap.net), um provedor não-oficial (automação sobre WhatsApp Web, não a Cloud API da Meta); mais simples e rápido de configurar que a API oficial, mas com risco de o número ser suspenso pela Meta a qualquer momento; alternável para o mock via `facilitei.notification.provider`
- [x] Cobrança da própria assinatura SaaS — 3 planos fixos (Básico/Profissional/Premium, limitados por número de profissionais), trial de 30 dias, cobrança Pix automática recorrente via chave Asaas da própria plataforma (pacote `billing`), webhook dedicado, bloqueio leve (só ações de novo uso) quando inadimplente/cancelada, cancelamento pela UI
- [x] Relatórios básicos para o dono do negócio: faturamento do período, taxa de não comparecimento, clientes recorrentes — exigiu ativar o cancelamento manual (`PaymentStatus.CANCELADO`, já existia no enum sem nenhum código usá-lo) e adicionar marcação manual de comparecimento na tela Agenda, já que o sistema não tinha nenhum registro de comparecimento antes disso

### Infraestrutura
- [x] CI/CD — build e testes automáticos a cada push
- [x] Observabilidade: logs estruturados (suporte nativo do Spring Boot 4, `logging.structured.format.console`) + correlation id por requisição, monitoramento de erro via Sentry (SDK core, DSN vazio por padrão até uma conta ser criada) — falta só o "alerta" de fato (`/actuator/health` já existe, mas disparar notificação de indisponibilidade real só faz sentido quando houver deploy em produção, ver item abaixo)
- [x] Testes end-to-end do fluxo completo (Playwright), não só unitários — integrado ao CI (GitHub Actions): sobe Postgres como service container, o backend real em background (aguardando `/actuator/health` responder `UP`) e o frontend via `ng serve`; o teste do fluxo "com sinal" continua pulando sozinho no CI (não há chave sandbox da Asaas nesse ambiente), só os fluxos que não dependem dela rodam de fato

### Jurídico & financeiro
- [ ] Contrato entre plataforma e cada negócio definindo quem é controlador e quem é operador dos dados (LGPD)
- [ ] Emissão de nota fiscal sobre a assinatura cobrada dos negócios

## Fase 3 — Aquisição ativa *(~3–6 meses)*

### Produto
- [ ] Cliente final poder reagendar/cancelar sozinho pelo link público

### Infraestrutura
- [ ] Ambiente de staging separado de produção
- [ ] Teste de carga antes de qualquer campanha de aquisição maior

### Go-to-market
- [x] Site institucional + página de vendas (hoje só existe o painel, sem "cara" de produto) — landing page estática (HTML/CSS puro, sem framework, pra ficar 100% indexável por buscadores e agentes de IA) em [`landing/`](landing/index.html), com SEO (meta tags, sitemap) e GEO/AEO (JSON-LD `FAQPage`, `llms.txt`) já implementados; falta só decidir o roteamento real (domínio raiz → landing, subdomínio/subcaminho → app Angular) quando o deploy em produção acontecer
- [ ] Canal de suporte (WhatsApp Business próprio ou help desk simples)
- [ ] Diferenciação clara frente a concorrentes estabelecidos (Trinks, Fixed, Simples Agenda, Booksy) — o diferencial natural é o sinal via Pix nativo, sem link de terceiros
- [ ] Ajustar preço com dados reais de uso das fases anteriores

## Fase 4 — Escala *(6–12 meses+)*

- [ ] Múltiplos usuários admin por negócio, com permissões por funcionário
- [ ] Múltiplos serviços numa única reserva (decisão consciente de adiar em etapas anteriores — reavaliar com demanda real)
- [ ] CRM leve / histórico de clientes
- [ ] Domínio próprio / white-label por negócio
- [ ] Parcerias com criadores de conteúdo do nicho (barbeiros, cabeleireiros)
- [ ] Avaliar seguro de responsabilidade civil se o volume intermediado crescer
- [x] **Portal do cliente + diretório de negócios**: cliente final loga por telefone + código de verificação via WhatsApp (sem senha), navega um diretório dos negócios que optaram por aparecer (opt-in — o link direto `/agendar/:slug` continua sendo o principal, isso é complemento) e agenda cruzando vários negócios, com histórico próprio ("Minhas reservas"). Implementado: entidade `Cliente` (telefone único, sem senha), tabela `codigos_verificacao` (OTP com expiração/tentativas), novo role JWT `CLIENTE` (sem `tenantId` — resolvido por endpoint, não por token), campos `categoria/cidade/descricao/visivel_no_diretorio` no `Tenant` (todos opt-in, `DEFAULT FALSE`), endpoint `GET /api/public/directory`, `bookings.cliente_id` (nullable, sem backfill do histórico anterior), portal em `/portal/**` no frontend e aba "Diretório" no painel admin. Envio do código via `NotificationService`/MyZap existente (provider `console` em dev, loga o código). Fora de escopo por ora: upload de foto/logo, avaliações, geolocalização real (só cidade em texto), rate limiter distribuído (mesma limitação single-instance do login já documentada).

---

## Modelo de monetização

| Modelo | Como funciona | Trade-off |
|---|---|---|
| **Assinatura fixa (implementado)** | R$ 49–149/mês por plano (Básico/Profissional/Premium), limitado por número de profissionais; cobrança Pix automática via chave Asaas da própria plataforma, trial de 30 dias | Receita previsível, mas é barreira de entrada para negócio pequeno. É o único modelo compatível com o BYOPP tal como está — como o sinal cai direto na conta do tenant, a plataforma não tem como cobrar automaticamente uma taxa % por transação |
| Taxa por transação | 2–4% sobre cada sinal Pix cobrado | Exigiria voltar ao modelo de split/marketplace (decidido não seguir por enquanto) para conseguir reter uma fatia automaticamente |
| Híbrido | Assinatura baixa + taxa pequena por transação, trial de 14–30 dias | Modelo dos concorrentes já validados no Brasil, mas inviável sem split — reavaliar só se o BYOPP se mostrar insuficiente |

## Riscos principais

- **Regulatório**: reduzido pelo modelo BYOPP (a plataforma nunca custodia o sinal do cliente final), mas ainda existe jurídico real a resolver — contrato definindo quem é controlador/operador dos dados (LGPD) e Termos de Uso deixando claro esse modelo de recebimento.
- **Concorrência já estabelecida**: Trinks, Fixed, Simples Agenda e Booksy já são conhecidos no Brasil — o diferencial precisa ficar claro no primeiro contato com o cliente.
- **Custo de aquisição**: pequenos negócios têm ticket baixo e demandam bastante suporte — validar o custo real de conquistar um cliente antes de escalar investimento em marketing.

## Primeiros passos concretos (próximas 2–3 semanas)

1. [ ] Conversar com um contador sobre CNPJ e enquadramento
2. [ ] Revisar a minuta de Termos de Uso e Política de Privacidade com contador/advogado (rascunho já escrito, ver `docs/`) — 5 pontos de conteúdo já resolvidos com profissional e tabela de planos preenchida, falta só formalizar CNPJ/razão social/endereço/foro/e-mail e aprovar o documento final
3. [x] ~~Escolher onde hospedar em produção~~ — VPS Hostinger (Boston/EUA) e domínio via Registro.br já decididos; falta subir o ambiente de fato (Postgres, domínio, HTTPS)
4. [ ] Conversar com 5 donos de barbearia/salão reais — mostrar o produto (inclusive como configurar o recebimento Pix), entender objeções, ainda não vender
