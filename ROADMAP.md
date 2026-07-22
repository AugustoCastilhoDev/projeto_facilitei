# Roadmap — Facilitei: de MVP a produto vendável

> Como usar: marque `[x]` conforme for concluindo cada item. Sinta-se livre para editar, reordenar, adicionar notas/datas ou remover itens que deixarem de fazer sentido — este documento é vivo, não um contrato fechado.

O que existe hoje é uma base técnica sólida (agendamento multi-tenant, sinal via Pix, painel admin, ~60 testes automatizados). O que falta para vender de verdade não é código — é modelo de negócio, dinheiro caindo no lugar certo, e um mínimo de jurídico. Duas coisas são **bloqueadoras** antes do primeiro cliente pagante: split de pagamento via Asaas e um mínimo de Termos de Uso/LGPD.

Ver também: [README.md](README.md) (arquitetura técnica e como rodar o projeto).

---

## Onde estamos vs. onde precisa chegar

| Hoje (MVP) | Produto vendável |
|---|---|
| Cadastro de negócio só via API (curl) | Onboarding self-service com formulário |
| 1 profissional/cadeira por negócio | Múltiplos profissionais, cada um com sua agenda |
| Sinal cai na conta Asaas da plataforma | Split automático para a conta do próprio negócio |
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
- [ ] Onboarding self-service — formulário público de cadastro de negócio (hoje só existe via curl)

### Infraestrutura
- [ ] Deploy em produção: Postgres gerenciado com backup, domínio, HTTPS
- [ ] Testar o webhook da Asaas com URL pública de verdade (hoje só foi simulado localmente)
- [ ] Hardening básico: rate limiting no login, segredos fora de arquivo `.yml` (usar secrets manager do provedor de hospedagem)

### Jurídico & financeiro
- [ ] Conversar com contador sobre CNPJ e enquadramento tributário correto (MEI provavelmente não serve para intermediar pagamento de terceiros)
- [ ] Escrever/contratar Termos de Uso + Política de Privacidade (LGPD)
- [ ] Formalizar com a Asaas o modelo de "plataforma com split de pagamento" (cadastro específico, diferente de conta comum)
- [ ] Implementar o split de pagamento em si (subconta Asaas por tenant)

### Go-to-market
- [ ] Nada de aquisição paga ainda — só continuar validando com early adopters da Fase 0

## Fase 2 — Produto completo *(~2–3 meses)*

### Produto
- [ ] Suporte a múltiplos profissionais/recursos por negócio (hoje o modelo é uma cadeira só)
- [ ] WhatsApp real via API oficial (Meta Cloud API, Twilio ou Zenvia), substituindo o mock de console
- [ ] Cobrança da própria assinatura SaaS — planos, período de teste, cancelamento
- [ ] Relatórios básicos para o dono do negócio: faturamento do período, taxa de não comparecimento, clientes recorrentes

### Infraestrutura
- [ ] CI/CD — build e testes automáticos a cada push
- [ ] Observabilidade: logs estruturados, monitoramento de erro (ex.: Sentry), alerta de indisponibilidade
- [ ] Testes end-to-end do fluxo completo (Playwright/Cypress), não só unitários

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
| Assinatura fixa | R$ 49–149/mês por plano, limitado por número de profissionais | Receita previsível, mas é barreira de entrada para negócio pequeno |
| Taxa por transação | 2–4% sobre cada sinal Pix cobrado | Fácil de vender ("só paga se usar"), mas receita variável e difícil de prever |
| **Híbrido (recomendado)** | Assinatura baixa + taxa pequena por transação, trial de 14–30 dias | Mais complexo de comunicar, mas é o modelo dos concorrentes já validados no Brasil |

## Riscos principais

- **Regulatório**: custódia e split de dinheiro de terceiros tem implicação legal real — não pular essa parte por pressa de lançar.
- **Concorrência já estabelecida**: Trinks, Fixed, Simples Agenda e Booksy já são conhecidos no Brasil — o diferencial precisa ficar claro no primeiro contato com o cliente.
- **Custo de aquisição**: pequenos negócios têm ticket baixo e demandam bastante suporte — validar o custo real de conquistar um cliente antes de escalar investimento em marketing.

## Primeiros passos concretos (próximas 2–3 semanas)

1. [ ] Conversar com um contador sobre CNPJ e enquadramento, antes de processar dinheiro de terceiros de verdade
2. [ ] Abrir uma conversa com o suporte da Asaas sobre o modelo de "plataforma com split de pagamento"
3. [ ] Escrever (ou contratar) uma versão mínima de Termos de Uso e Política de Privacidade
4. [ ] Escolher onde hospedar em produção e subir o ambiente com domínio e HTTPS
5. [ ] Conversar com 5 donos de barbearia/salão reais — mostrar o produto, entender objeções, ainda não vender
