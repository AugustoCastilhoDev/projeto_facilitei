-- Nova agenda (fase 2a): disponibilidade CALCULADA a partir de horarios de
-- trabalho + agendamentos com inicio/fim, no lugar dos slots pre-gerados.
-- Aditiva de proposito: slots/bookings continuam existindo ate o codigo antigo
-- ser removido (fase 2e), para o app seguir funcionando a cada fase.
--
-- btree_gist permite combinar igualdade (profissional_id WITH =) com
-- sobreposicao de intervalos (&&) numa mesma constraint de exclusao. E uma
-- extensao "trusted" desde o PostgreSQL 13: o dono do banco consegue cria-la
-- sem ser superusuario.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Parametros da agenda de cada negocio, com padroes pensados para barbearia/salao.
ALTER TABLE tenants ADD COLUMN fuso_horario VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo';
ALTER TABLE tenants ADD COLUMN intervalo_agenda_min INT NOT NULL DEFAULT 30;
ALTER TABLE tenants ADD COLUMN antecedencia_minima_min INT NOT NULL DEFAULT 60;
ALTER TABLE tenants ADD COLUMN horizonte_dias INT NOT NULL DEFAULT 60;

ALTER TABLE tenants ADD CONSTRAINT chk_tenants_intervalo_agenda CHECK (intervalo_agenda_min IN (5, 10, 15, 20, 30, 60));
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_antecedencia CHECK (antecedencia_minima_min >= 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_horizonte CHECK (horizonte_dias BETWEEN 1 AND 365);

-- Alvo da FK composta de agendamentos: garante no banco que um agendamento nunca
-- aponte para um profissional de OUTRO negocio (isolamento multi-tenant que hoje
-- so existe na aplicacao).
ALTER TABLE profissionais ADD CONSTRAINT uq_profissionais_id_tenant UNIQUE (id, tenant_id);

-- Expediente semanal do profissional. Varios intervalos no mesmo dia
-- representam pausas (ex.: 09-12 e 14-18 para o almoco).
CREATE TABLE horarios_trabalho (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profissional_id BIGINT      NOT NULL REFERENCES profissionais(id) ON DELETE CASCADE,
    dia_semana      VARCHAR(10) NOT NULL,
    inicio          TIME        NOT NULL,
    fim             TIME        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_horarios_trabalho_dia CHECK (dia_semana IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT chk_horarios_trabalho_periodo CHECK (fim > inicio),
    -- dois intervalos do mesmo profissional no mesmo dia nao podem se sobrepor
    CONSTRAINT ex_horarios_trabalho_sem_sobreposicao EXCLUDE USING gist (
        profissional_id WITH =,
        dia_semana WITH =,
        tsrange(DATE '2000-01-01' + inicio, DATE '2000-01-01' + fim) WITH &&
    )
);

-- Folgas, ferias, consultas medicas etc.: periodos em que o profissional nao atende.
CREATE TABLE bloqueios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profissional_id BIGINT      NOT NULL REFERENCES profissionais(id) ON DELETE CASCADE,
    inicio          TIMESTAMPTZ NOT NULL,
    fim             TIMESTAMPTZ NOT NULL,
    motivo          VARCHAR(150),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bloqueios_periodo CHECK (fim > inicio)
);

CREATE INDEX idx_bloqueios_profissional_inicio ON bloqueios (profissional_id, inicio);

CREATE TABLE agendamentos (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- identificador que vai em links publicos; o id sequencial nunca sai da API publica
    codigo_publico      UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           BIGINT        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    profissional_id     BIGINT        NOT NULL,
    cliente_id          BIGINT        REFERENCES clientes(id),
    cliente_nome        VARCHAR(150)  NOT NULL,
    cliente_telefone    VARCHAR(20)   NOT NULL,
    cliente_cpf_cnpj    VARCHAR(20),
    inicio              TIMESTAMPTZ   NOT NULL,
    fim                 TIMESTAMPTZ   NOT NULL,
    status              VARCHAR(30)   NOT NULL,
    origem              VARCHAR(20)   NOT NULL,
    valor_total         NUMERIC(10,2) NOT NULL,
    valor_sinal         NUMERIC(10,2) NOT NULL DEFAULT 0,
    -- ate quando um agendamento AGUARDANDO_* segura o horario antes de expirar
    expira_em           TIMESTAMPTZ,
    asaas_payment_id    VARCHAR(100),
    pix_payload         TEXT,
    sinal_confirmado_em TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_agendamentos_codigo_publico UNIQUE (codigo_publico),
    CONSTRAINT fk_agendamentos_profissional FOREIGN KEY (profissional_id, tenant_id)
        REFERENCES profissionais (id, tenant_id),
    CONSTRAINT chk_agendamentos_periodo CHECK (fim > inicio),
    CONSTRAINT chk_agendamentos_status CHECK (status IN (
        'AGUARDANDO_PAGAMENTO', 'AGUARDANDO_CONFIRMACAO', 'CONFIRMADO', 'CONCLUIDO', 'FALTOU',
        'EXPIRADO', 'CANCELADO_CLIENTE', 'CANCELADO_NEGOCIO')),
    CONSTRAINT chk_agendamentos_origem CHECK (origem IN ('PUBLICO', 'PORTAL', 'PAINEL')),
    CONSTRAINT chk_agendamentos_valores CHECK (valor_total >= 0 AND valor_sinal >= 0 AND valor_sinal <= valor_total),
    -- um horario segurado sem prazo ficaria preso para sempre (a familia de bug que
    -- a agenda antiga tinha): o banco nao deixa existir um AGUARDANDO_* sem expiracao
    CONSTRAINT chk_agendamentos_espera_tem_prazo CHECK (
        status NOT IN ('AGUARDANDO_PAGAMENTO', 'AGUARDANDO_CONFIRMACAO') OR expira_em IS NOT NULL),
    -- FONTE DE VERDADE contra double booking: um profissional nao pode ter dois
    -- agendamentos que ocupam horario com periodos sobrepostos. '[)' deixa horarios
    -- encostados (10:00-10:30 e 10:30-11:00) valerem. Cancelado/expirado nao entra no
    -- predicado, entao liberar um horario e reserva-lo de novo funciona. A lista de
    -- status tem que bater com AgendamentoStatus.ocupaHorario() (ha um teste para isso).
    CONSTRAINT ex_agendamento_sem_sobreposicao EXCLUDE USING gist (
        profissional_id WITH =,
        tstzrange(inicio, fim, '[)') WITH &&
    ) WHERE (status IN ('AGUARDANDO_PAGAMENTO', 'AGUARDANDO_CONFIRMACAO', 'CONFIRMADO', 'CONCLUIDO', 'FALTOU'))
);

CREATE INDEX idx_agendamentos_tenant_inicio ON agendamentos (tenant_id, inicio);
CREATE INDEX idx_agendamentos_cliente ON agendamentos (cliente_id) WHERE cliente_id IS NOT NULL;
-- o id do pagamento na Asaas e globalmente unico; o webhook busca por ele
CREATE UNIQUE INDEX uq_agendamentos_asaas_payment_id ON agendamentos (asaas_payment_id) WHERE asaas_payment_id IS NOT NULL;
-- varredura de expiracao (so olha quem esta esperando)
CREATE INDEX idx_agendamentos_expiracao ON agendamentos (expira_em)
    WHERE status IN ('AGUARDANDO_PAGAMENTO', 'AGUARDANDO_CONFIRMACAO');

-- Servicos de um agendamento ("Corte + Barba"). Nome, duracao e preco sao COPIADOS
-- na hora da reserva: mudar o preco de um servico depois nao pode alterar o
-- historico nem o faturamento dos relatorios.
CREATE TABLE agendamento_servicos (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    agendamento_id BIGINT        NOT NULL REFERENCES agendamentos(id) ON DELETE CASCADE,
    servico_id     BIGINT        REFERENCES services(id) ON DELETE SET NULL,
    ordem          INT           NOT NULL,
    nome           VARCHAR(150)  NOT NULL,
    duracao_min    INT           NOT NULL CHECK (duracao_min > 0),
    preco          NUMERIC(10,2) NOT NULL CHECK (preco >= 0),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_agendamento_servicos_ordem UNIQUE (agendamento_id, ordem)
);
