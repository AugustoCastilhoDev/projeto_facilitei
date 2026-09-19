# Nova agenda — modelo e contrato da API

> Estado: backend pronto (fases 2a e 2b). Convive com a agenda antiga (`slots`/`bookings`) até a fase 2e, quando o código antigo é removido e o README passa a descrever só este modelo.

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
AGUARDANDO_PAGAMENTO ──pagou──▶ CONFIRMADO ──▶ CONCLUIDO | FALTOU
        │  (segura o horário 15 min)   ▲
        ├──prazo──▶ EXPIRADO ──pagou tarde, horário ainda livre──┘
        └──dono cancela──▶ CANCELADO_NEGOCIO
```

Sem sinal, nasce `CONFIRMADO`. `AGUARDANDO_CONFIRMACAO` (Pix próprio, confirmação manual) e `CANCELADO_CLIENTE` entram nas próximas fases. Ocupam o horário: `AGUARDANDO_*`, `CONFIRMADO`, `CONCLUIDO`, `FALTOU`.

## API pública (sem login) — `/api/public/tenants/{slug}/agenda`

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/profissionais?servicoIds=1,2` | Quem realiza **todos** os serviços escolhidos → `[{id, nome}]` |
| GET | `/disponibilidade?servicoIds=1,2&profissionalId=&data=2026-09-21` | Horários livres do dia; sem `profissionalId` = "qualquer profissional" (cada horário aparece uma vez, com o primeiro profissional livre). Datas com deslocamento do fuso: `2026-09-21T10:00:00-03:00` |
| POST | `/agendamentos` | Reserva. Body: `servicoIds[]`, `profissionalId?`, `inicio` (exatamente um horário devolvido acima), `clienteNome`, `clienteTelefone`, `clienteCpfCnpj?` (só exigido se o negócio cobra sinal pela Asaas) |
| GET | `/agendamentos/{codigo}` | Estado da reserva (polling até o pagamento confirmar). Enquanto pendente, devolve o Pix e rebusca o QR (sobrevive a F5) |

O agendamento é identificado só pelo `codigo` (UUID), nunca pelo id sequencial, e a resposta pública não traz o telefone do cliente.

Resposta de `POST`/`GET agendamentos`: `codigo`, `status`, `inicio`, `fim`, `profissionalNome`, `servicos[]`, `valorTotal`, `valorSinal`, `expiraEm`, `pix` (`{copiaECola, qrCodeBase64}` só enquanto `AGUARDANDO_PAGAMENTO`, senão `null`).

## API do painel (JWT ADMIN) — `/api/admin/tenants/{tenantId}`

O `tenantId` do path precisa ser o do token (senão `403`).

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/agendamentos?de=&ate=&profissionalId=` | Agenda do período (até 62 dias), todos os status, com dados do cliente |
| POST | `/agendamentos` | Cria pelo painel (cliente do balcão/telefone): ignora expediente/grade/antecedência, não cobra sinal, nasce `CONFIRMADO`. Continua barrado por sobreposição (`409`) |
| POST | `/agendamentos/{id}/confirmar-pagamento` · `/cancelar` · `/concluir` · `/faltou` | Ciclo de vida. `concluir`/`faltou` só depois de o atendimento começar |
| GET/PUT | `/profissionais/{id}/horarios` | Expediente semanal; o PUT **substitui a semana inteira** (`[{diaSemana: "MONDAY", inicio: "09:00:00", fim: "12:00:00"}]`). Sobreposição no mesmo dia → `400` |
| GET/POST | `/profissionais/{id}/bloqueios` | Folgas em andamento/futuras. O POST devolve `agendamentosAfetados`: bloquear **não** cancela ninguém, só avisa quantos já estavam marcados |
| DELETE | `/profissionais/{id}/bloqueios/{bloqueioId}` | Remove o bloqueio |
| GET/PUT | `/agenda-config` | `fusoHorario`, `intervaloAgendaMin` (5/10/15/20/30/60), `antecedenciaMinimaMin`, `horizonteDias` |

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
- **`Clock` injetável:** controllers e jobs recebem o "agora" por dependência, então os testes fixam a data.

## Limitações conhecidas (para as próximas fases)

- O dono só é avisado de pagamento tardio por log/Sentry — falta o telefone do dono no cadastro para avisá-lo por WhatsApp (fase 2c).
- O turno de trabalho não atravessa a meia-noite.
- Cancelar/remarcar pelo próprio cliente ainda não existe (fase 3).
- O endpoint público de reserva não tem rate limit por IP; o teto é de 2 reservas em espera por telefone e o prazo curto (15 min) de cada uma.
