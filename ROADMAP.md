# Roadmap — Facilitei: de MVP a produto vendável

> Como usar: marque `[x]` conforme for concluindo cada item. Sinta-se livre para editar, reordenar, adicionar notas/datas ou remover itens que deixarem de fazer sentido — este documento é vivo, não um contrato fechado.

O que existe hoje é uma base técnica sólida (agendamento multi-tenant, sinal via Pix, painel admin, onboarding self-service, ~65 testes automatizados). O sinal já cai direto na conta Asaas de cada negócio (modelo "traga sua própria conta de pagamento" — BYOPP, ver README) — decisão tomada no lugar do split/marketplace originalmente cogitado, porque não exige formalização prévia com a Asaas nem custodia dinheiro de terceiros. O que ainda falta para vender de verdade não é código — é um mínimo de jurídico (Termos de Uso/LGPD) e decidir onde hospedar em produção.

Ver também: [README.md](README.md) (arquitetura técnica e como rodar o projeto).

---

## Onde estamos vs. onde precisa chegar

| Hoje (MVP) | Produto vendável |
|---|---|
| ~~Cadastro de negócio só via API (curl)~~ Onboarding self-service ✅ | — |
| 1 profissional/cadeira por negócio | Múltiplos profissionais, cada um com sua agenda |
| ~~Sinal cai na conta Asaas da plataforma~~ Sinal direto na conta do negócio (BYOPP) ✅ | Onboarding assistido/automatizado da conta Asaas do tenant (hoje é manual, copiar/colar a chave) |
| Sem cobrança pela assinatura do SaaS | Billing recorrente, planos e trial |
| Roda só localmente (docker compose) | Deploy em produção, com monitoramento |
| Sem Termos de Uso nem Política de Privacidade | LGPD tratada, contrato entre plataforma e negócio |
| Notificação = log no console | WhatsApp de verdade, via API oficial |

---

## Fase 0 — Validar com o que existe *(contínuo)*

- [ ] Conseguir 5–10 negócios reais (barbearias/salões pequenos) usando o MVP atual, de graça ou com desconto
- [ ] Cobrar a mensalidade combinada manualmente, fora do sistema, enquanto o billing próprio não existe
- [ ] Coletar feedback de uso real antes de investir pesado nas próximas fases

## Fase 1 — Fechar os bloqueadores *(~4–6 semanas)*

### Produto
- [x] Onboarding self-service — formulário público de cadastro de negócio (hoje só existe via curl)

### Infraestrutura
- [ ] Deploy em produção: Postgres gerenciado com backup, domínio, HTTPS
- [x] Testar o webhook da Asaas com URL pública de verdade (hoje só foi simulado localmente)
- [x] Hardening básico: rate limiting no login, segredos fora de arquivo `.yml` (usar secrets manager do provedor de hospedagem)

### Jurídico & financeiro
- [ ] Conversar com contador sobre CNPJ e enquadramento tributário correto
- [ ] Escrever/contratar Termos de Uso + Política de Privacidade (LGPD) — inclui deixar claro que a Facilitei não custodia o pagamento do sinal (modelo BYOPP), só a assinatura do SaaS
- ~~Formalizar com a Asaas o modelo de "plataforma com split de pagamento"~~ — decidido não seguir por esse caminho: o modelo BYOPP não exige formalização nenhuma com a Asaas
- [x] Implementar o recebimento direto na conta do próprio tenant (modelo BYOPP: chave Asaas por tenant, cifrada em repouso, webhook autenticado por tenant) — ver README/`docs/configurar-pagamentos.md`

### Go-to-market
- [ ] Nada de aquisição paga ainda — só continuar validando com early adopters da Fase 0

## Fase 2 — Produto completo *(~2–3 meses)*

### Produto
- [ ] Suporte a múltiplos profissionais/recursos por negócio (hoje o modelo é uma cadeira só)
- [ ] WhatsApp real via API oficial (Meta Cloud API, Twilio ou Zenvia), substituindo o mock de console
- [ ] Cobrança da própria assinatura SaaS — planos, período de teste, cancelamento
- [ ] Relatórios básicos para o dono do negócio: faturamento do período, taxa de não comparecimento, clientes recorrentes

### Infraestrutura
- [x] CI/CD — build e testes automáticos a cada push
- [ ] Observabilidade: logs estruturados, monitoramento de erro (ex.: Sentry), alerta de indisponibilidade
- [x] Testes end-to-end do fluxo completo (Playwright/Cypress), não só unitários — rodando localmente; ainda não integrado ao CI (exigiria Postgres + backend real no workflow)

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
- [ ] Site institucional + página de vendas (hoje só existe o painel, sem "cara" de produto)
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

---

## Modelo de monetização

| Modelo | Como funciona | Trade-off |
|---|---|---|
| **Assinatura fixa (implementado como BYOPP)** | R$ 49–149/mês por plano, limitado por número de profissionais | Receita previsível, mas é barreira de entrada para negócio pequeno. É o único modelo compatível com o BYOPP tal como está — como o sinal cai direto na conta do tenant, a plataforma não tem como cobrar automaticamente uma taxa % por transação |
| Taxa por transação | 2–4% sobre cada sinal Pix cobrado | Exigiria voltar ao modelo de split/marketplace (decidido não seguir por enquanto) para conseguir reter uma fatia automaticamente |
| Híbrido | Assinatura baixa + taxa pequena por transação, trial de 14–30 dias | Modelo dos concorrentes já validados no Brasil, mas inviável sem split — reavaliar só se o BYOPP se mostrar insuficiente |

## Riscos principais

- **Regulatório**: reduzido pelo modelo BYOPP (a plataforma nunca custodia o sinal do cliente final), mas ainda existe jurídico real a resolver — contrato definindo quem é controlador/operador dos dados (LGPD) e Termos de Uso deixando claro esse modelo de recebimento.
- **Concorrência já estabelecida**: Trinks, Fixed, Simples Agenda e Booksy já são conhecidos no Brasil — o diferencial precisa ficar claro no primeiro contato com o cliente.
- **Custo de aquisição**: pequenos negócios têm ticket baixo e demandam bastante suporte — validar o custo real de conquistar um cliente antes de escalar investimento em marketing.

## Primeiros passos concretos (próximas 2–3 semanas)

1. [ ] Conversar com um contador sobre CNPJ e enquadramento
2. [ ] Escrever (ou contratar) uma versão mínima de Termos de Uso e Política de Privacidade
3. [ ] Escolher onde hospedar em produção e subir o ambiente com domínio e HTTPS
4. [ ] Conversar com 5 donos de barbearia/salão reais — mostrar o produto (inclusive como configurar o recebimento Pix), entender objeções, ainda não vender
