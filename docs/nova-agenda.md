# Nova agenda — modelo e contrato da API

> Estado: backend pronto (fases 2a, 2b e 2c). Convive com a agenda antiga (`slots`/`bookings`) até a fase 2e, quando o código antigo é removido e o README passa a descrever só este modelo.

## Ideia central

Não há mais "slots" pré-gerados. A disponibilidade é **calculada** a partir do expediente semanal do profissional, dos bloqueios (folgas) e dos agendamentos existentes. Quem impede dois clientes de levarem o mesmo horário é uma **constraint de exclusão do PostgreSQL** (`ex_agendamento_sem_sobreposicao`, com `btree_gist`), não código Java: o `INSERT` do segundo cliente falha e vira `409`.

```
horarios_trabalho (profissional, dia da semana, início, fim)   ← expediente semanal, vários intervalos/dia (almoço)
bloqueios         (profissional, início, fim, motivo)          ← folgas, férias
agendamentos      (profissional, início, fim, status, sinal…)  ← 1 agendamento = N serviços
agendamento_servicos (nome, duração, preço COPIADOS)           ← histórico imune a reajuste de preço
```

Parâmetros por negócio (`GET/PUT /agenda-config`): fuso horário, grade de horários (5–60 min, padrão 30), antecedência mínima (padrão 60 min) e horizonte (padrão 60 dias).

## Estados do agendamento

```
AGUARDANDO_PAGAMENTO ──pagou (webhook Asaas)──▶ CONFIRMADO ──▶ CONCLUIDO | FALTOU
   │  (segura o horário 15 min)                      ▲   ▲
   │                                                 │   │
   ├──"já paguei" (Pix próprio)──▶ AGUARDANDO_CONFIRMACAO ──dono confirma──┘   │
   │                                  │  (segura o horário 60 min)             │
   │                                  └──dono recusa──▶ CANCELADO_NEGOCIO      │
   ├──prazo──▶ EXPIRADO ──pagou tarde / dono confirma, horário ainda livre─────┘
   └──dono cancela──▶ CANCELADO_NEGOCIO
```

Sem sinal, nasce `CONFIRMADO`. `CANCELADO_CLIENTE` entra na fase 3. Ocupam o horário: `AGUARDANDO_*`, `CONFIRMADO`, `CONCLUIDO`, `FALTOU`.

O meio de cobrança fica gravado em cada agendamento (`meioCobranca`: `ASAAS` ou `PIX_PROPRIO`): se o dono trocar de meio depois, a reserva em andamento segue as regras do meio com que nasceu.

## API pública (sem login) — `/api/public/tenants/{slug}/agenda`

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/profissionais?servicoIds=1,2` | Quem realiza **todos** os serviços escolhidos → `[{id, nome}]` |
| GET | `/disponibilidade?servicoIds=1,2&profissionalId=&data=2026-09-21` | Horários livres do dia; sem `profissionalId` = "qualquer profissional" (cada horário aparece uma vez, com o primeiro profissional livre). Datas com deslocamento do fuso: `2026-09-21T10:00:00-03:00` |
| POST | `/agendamentos` | Reserva. Body: `servicoIds[]`, `profissionalId?`, `inicio` (exatamente um horário devolvido acima), `clienteNome`, `clienteTelefone`, `clienteCpfCnpj?` (só exigido se o negócio cobra sinal pela Asaas) |
| GET | `/agendamentos/{codigo}` | Estado da reserva (polling até o pagamento confirmar). Enquanto pendente, devolve o Pix e rebusca o QR (sobrevive a F5) |
| POST | `/agendamentos/{codigo}/informar-pagamento` | Só Pix próprio: o cliente avisa que pagou ("Já paguei"). Segura o horário por mais 60 min e chama o dono no WhatsApp. Idempotente. Resposta igual à do `GET`. `400` se o pagamento é automático (Asaas); `409` se a reserva já expirou e outra pessoa ficou com o horário |

O agendamento é identificado só pelo `codigo` (UUID), nunca pelo id sequencial, e a resposta pública não traz o telefone do cliente.

Resposta de `POST`/`GET agendamentos`: `codigo`, `status`, `inicio`, `fim`, `profissionalNome`, `servicos[]`, `valorTotal`, `valorSinal`, `expiraEm`, `confirmacaoManual`, `pix` (`{copiaECola, qrCodeBase64}` só enquanto `AGUARDANDO_PAGAMENTO`, senão `null`).

- `confirmacaoManual = true` significa Pix na chave do dono: a tela mostra o botão **"Já paguei"** depois do QR e, em `AGUARDANDO_CONFIRMACAO`, o aviso "aguardando o estabelecimento confirmar até `expiraEm`".
- No Pix próprio **`pix.qrCodeBase64` é sempre `null`**: o frontend desenha o QR a partir de `pix.copiaECola` (o texto do BR Code já é o conteúdo do QR). Na Asaas o QR vem pronto.

## API do painel (JWT ADMIN) — `/api/admin/tenants/{tenantId}`

O `tenantId` do path precisa ser o do token (senão `403`).

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/agendamentos?de=&ate=&profissionalId=` | Agenda do período (até 62 dias), todos os status, com dados do cliente, `pagamentoInformado`, `meioCobranca` e `referenciaPix` (o "FAC42" que o comprovante do banco mostra) |
| POST | `/agendamentos` | Cria pelo painel (cliente do balcão/telefone): ignora expediente/grade/antecedência, não cobra sinal, nasce `CONFIRMADO`. Continua barrado por sobreposição (`409`) |
| POST | `/agendamentos/{id}/confirmar-pagamento` · `/cancelar` · `/concluir` · `/faltou` | Ciclo de vida. `concluir`/`faltou` só depois de o atendimento começar |
| POST | `/agendamentos/{id}/recusar-pagamento` | O dono não achou o Pix no extrato: cancela, devolve o horário e avisa o cliente. Só vale depois de o cliente avisar que pagou (`AGUARDANDO_CONFIRMACAO`) |
| GET | `/recebimento` | Como o negócio recebe o sinal: `meioAtivo` (`ASAAS`, `PIX_PROPRIO` ou `null`), `asaasConfigurado`, `pixProprio`, `telefoneDono` |
| PUT · DELETE | `/recebimento/pix-proprio` | Cadastra/remove a chave Pix: `{tipoChave: CPF|CNPJ|TELEFONE|EMAIL|ALEATORIA, chave, nomeRecebedor?, cidadeRecebedor?}`. A chave é validada e normalizada; exige o WhatsApp do dono já cadastrado (`400` senão) |
| PUT | `/telefone-dono` | WhatsApp do dono `{telefone}` (guardado como `55DDDNUMERO`): para onde vão os pedidos de confirmação e os avisos de dinheiro sem horário. Também aceito, opcional, no cadastro (`telefoneDono`) |
| GET/PUT | `/profissionais/{id}/horarios` | Expediente semanal; o PUT **substitui a semana inteira** (`[{diaSemana: "MONDAY", inicio: "09:00:00", fim: "12:00:00"}]`). Sobreposição no mesmo dia → `400` |
| GET/POST | `/profissionais/{id}/bloqueios` | Folgas em andamento/futuras. O POST devolve `agendamentosAfetados`: bloquear **não** cancela ninguém, só avisa quantos já estavam marcados |
| DELETE | `/profissionais/{id}/bloqueios/{bloqueioId}` | Remove o bloqueio |
| GET/PUT | `/agenda-config` | `fusoHorario`, `intervaloAgendaMin` (5/10/15/20/30/60), `antecedenciaMinimaMin`, `horizonteDias` |

## Pix na chave do dono (fase 2c)

O negócio pode receber o sinal **direto na própria chave Pix**, sem conta em gateway. A Facilitei só monta o "Pix copia e cola" (BR Code com o valor do sinal e o identificador `FAC<id>`); como o banco **não avisa** ninguém quando o Pix cai, quem confirma é o dono.

```
1. cliente reserva        → AGUARDANDO_PAGAMENTO (15 min) + Pix copia e cola
2. cliente paga e toca "Já paguei"  → AGUARDANDO_CONFIRMACAO (60 min)
3. dono recebe no WhatsApp: "Pix a conferir: João avisou que pagou R$ 15,00 (Corte, 21/09 às 10:00).
                             Confira no seu banco (referência FAC42) e responda por este link: …/confirmar-sinal/<token>"
4. dono abre o link, confere o extrato e toca "Recebi" (CONFIRMADO) ou "Não recebi" (CANCELADO_NEGOCIO, horário volta e o cliente é avisado)
```

**Precedência:** quem tem a chave da Asaas **e** a chave Pix fica com a Asaas (confirma sozinha). Para usar o Pix próprio, o negócio não pode ter a chave da Asaas cadastrada.

### Link do dono — `/api/public/confirmacoes-sinal/{token}` (sem login)

O segredo do link é a autorização. É aleatório (256 bits), **diferente do `codigo` da reserva** (que o cliente conhece — se servisse, o cliente confirmaria o próprio pagamento) e só o SHA-256 dele fica no banco. Token inválido, de formato errado, do `codigo` do cliente ou substituído por um mais novo responde sempre o mesmo `404`.

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/{token}` | O que está sendo confirmado: `status`, `negocioNome`, `referencia` (FAC42), `clienteNome`, `inicio`, `fim`, `profissionalNome`, `servicos[]`, `valorSinal`, `pagamentoInformadoEm`, `expiraEm`, `podeConfirmar`, `podeRecusar`. Sem telefone do cliente |
| POST | `/{token}/confirmar` | "Recebi". Idempotente. Se a reserva já expirou, volta **só se o horário ainda estiver livre**; senão `409` ("devolva o sinal ao cliente") |
| POST | `/{token}/recusar` | "Não recebi". Idempotente. `400` se já foi confirmado (um link antigo não derruba uma reserva confirmada) |

**Contrato para o frontend (2d):** a rota do site é `/confirmar-sinal/:token` (o backend monta o link com `facilitei.frontend.base-url`, variável `FRONTEND_BASE_URL`); ela chama o `GET` ao abrir e mostra os botões conforme `podeConfirmar`/`podeRecusar`.

### Textos de WhatsApp desta fase

| Para | Quando |
|---|---|
| Cliente | Pedido de pagamento (no Pix próprio explica o "Já paguei"), confirmado, prazo acabou (texto diferente se ele já tinha avisado que pagou), recusado pelo dono ("não encontrou o pagamento… entre em contato") |
| Dono | Pix a conferir (com o link); **dinheiro que entrou sem horário** — pagamento tardio da Asaas com o horário já ocupado, pagamento de agendamento cancelado, ou "Já paguei" de uma reserva expirada cujo horário foi tomado. Traz nome e telefone do cliente para o dono devolver o valor |

## Códigos de erro (RFC 7807, campo `detail`)

| Código | Quando |
|---|---|
| `400` | Dados inválidos (`erros` por campo), regra de negócio (ex.: telefone inválido, expediente sobreposto) |
| `401` / `403` | Sem token / token de outro negócio |
| `404` | Recurso não existe **neste** negócio |
| `409` | O horário não está mais livre (perdeu a corrida ou nunca esteve na agenda) — escolha outro |
| `429` | Já tem 2 reservas aguardando pagamento neste negócio com esse telefone |
| `502` | O gateway de pagamento falhou; o horário foi devolvido |

## Decisões que valem lembrar

- **A checagem de conflito é do banco.** A listagem de horários é só um convite; o `INSERT` decide. Testado com 8 threads disputando o mesmo horário (1 vencedor).
- **Sem HTTP dentro de transação.** O checkout é: (1) segura o horário, (2) cria a cobrança no gateway, (3) vincula e avisa — três transações curtas, com a chamada de rede entre elas. Se o passo 2 falha, o horário volta na hora.
- **Notificação só depois do commit** (`@TransactionalEventListener(AFTER_COMMIT)`): o WhatsApp nunca mais sai de uma reserva que sofreu rollback.
- **Pagamento tardio:** se o Pix é pago depois de o prazo acabar, o agendamento volta **se o horário ainda estiver livre**; se outra pessoa já o ocupou, a reserva dela fica intacta e o caso vira alerta de erro (log + Sentry) para o dono devolver o sinal. Ao expirar/cancelar, a cobrança pendente é cancelada na Asaas para ninguém pagar por um horário que não é mais dele.
- **Webhook da Asaas responde `200` para pagamento desconhecido.** Na conta Asaas do próprio negócio entram cobranças que não são do Facilitei; um erro faria a Asaas pausar a fila do webhook.
- **Quem muda o estado trava a linha.** Painel, link do dono e webhook buscam o agendamento com `SELECT … FOR UPDATE` (`findWithLock…`); o job de expiração usa `SKIP LOCKED`. Sem isso, "confirmar" e "expirar" ao mesmo tempo se sobrescreviam (confirmado com o horário já entregue a outra pessoa). Testado: o job pula a reserva que está sendo decidida.
- **O dono pode confirmar depois de o prazo acabar.** O Pix caiu na conta dele; se o horário ainda está livre a reserva volta, senão a constraint do banco recusa (`409`) e a reserva de quem ficou com o horário fica intacta. O mesmo vale para o cliente que toca "Já paguei" atrasado.
- **Um só "Já paguei" gera um só pedido ao dono.** Repetir a chamada não troca o link nem manda outra mensagem. Além disso, reservar e avisar pagamento têm limite por IP (`429`): sem ele, quem varia o telefone digitado esgotaria a agenda de um negócio ou encheria o WhatsApp do dono - o teto de 2 reservas em espera por telefone não segura isso.
- **`Clock` injetável:** controllers e jobs recebem o "agora" por dependência, então os testes fixam a data.

## Limitações conhecidas (para as próximas fases)

- O pedido de confirmação é uma única mensagem: se o WhatsApp falhar naquele instante, o dono só vê a reserva em `AGUARDANDO_CONFIRMACAO` no painel (não há reenvio automático).
- O Pix estático não traz de volta o valor pago: se o cliente pagar outra quantia, quem percebe é o dono no extrato.
- O turno de trabalho não atravessa a meia-noite.
- Cancelar/remarcar pelo próprio cliente ainda não existe (fase 3).
- O limite por IP (reservas e "já paguei") é em memória, por instância: com mais de uma instância do backend cada uma conta o seu (o mesmo limite do login, ver `LoginRateLimiter`). Atrás de CGNAT (operadoras móveis) várias pessoas dividem um IP; os tetos padrão (10 por 10 min) sobram para uso real, e são ajustáveis por `AGENDA_LIMITE_RESERVAS_POR_IP` / `AGENDA_LIMITE_AVISOS_POR_IP`.
