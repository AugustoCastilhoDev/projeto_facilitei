-- Fase 2c: o negocio recebe o sinal na PROPRIA chave Pix (sem conta em gateway) e o
-- dono confirma o recebimento por um link enviado ao WhatsApp dele.

-- Onde o dono e avisado (pedido de confirmacao, pagamento tardio sem horario livre).
-- Guardado como o WhatsApp usa: so digitos, com o 55 na frente.
ALTER TABLE tenants ADD COLUMN telefone_dono VARCHAR(20);

-- Chave Pix do negocio. So o tipo + o valor normalizado (ex.: telefone "+5563999999999");
-- a chave nao e segredo (aparece no QR de qualquer cobranca), por isso vai em texto puro.
-- 77 = maior chave que cabe no campo 26 do BR Code; 25 e 15 sao os limites do nome e da
-- cidade do recebedor no mesmo padrao.
ALTER TABLE tenants ADD COLUMN pix_tipo_chave VARCHAR(10);
ALTER TABLE tenants ADD COLUMN pix_chave VARCHAR(77);
ALTER TABLE tenants ADD COLUMN pix_nome_recebedor VARCHAR(25);
ALTER TABLE tenants ADD COLUMN pix_cidade_recebedor VARCHAR(15);

ALTER TABLE tenants ADD CONSTRAINT chk_tenants_pix_tipo
    CHECK (pix_tipo_chave IS NULL OR pix_tipo_chave IN ('CPF', 'CNPJ', 'TELEFONE', 'EMAIL', 'ALEATORIA'));
-- ou ha chave completa (tipo, valor, nome e cidade do recebedor) ou nao ha nada: meia configuracao
-- geraria um Pix que o banco do cliente recusa
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_pix_completo CHECK (
    (pix_chave IS NULL AND pix_tipo_chave IS NULL AND pix_nome_recebedor IS NULL AND pix_cidade_recebedor IS NULL)
    OR (pix_chave IS NOT NULL AND pix_tipo_chave IS NOT NULL
        AND pix_nome_recebedor IS NOT NULL AND pix_cidade_recebedor IS NOT NULL));

-- Por qual meio o sinal DESTE agendamento foi cobrado. Fica guardado no agendamento (e nao
-- deduzido da configuracao atual do negocio) porque o dono pode trocar de meio depois, e um
-- agendamento em andamento tem que seguir as regras do meio com que nasceu.
ALTER TABLE agendamentos ADD COLUMN meio_cobranca VARCHAR(20);
UPDATE agendamentos SET meio_cobranca = 'ASAAS' WHERE asaas_payment_id IS NOT NULL;
ALTER TABLE agendamentos ADD CONSTRAINT chk_agendamentos_meio_cobranca
    CHECK (meio_cobranca IS NULL OR meio_cobranca IN ('ASAAS', 'PIX_PROPRIO'));

-- Quando o cliente disse "ja paguei" (Pix proprio: o dono ainda tem que conferir no banco).
ALTER TABLE agendamentos ADD COLUMN pagamento_informado_em TIMESTAMPTZ;
ALTER TABLE agendamentos ADD CONSTRAINT chk_agendamentos_confirmacao_tem_aviso CHECK (
    status <> 'AGUARDANDO_CONFIRMACAO' OR pagamento_informado_em IS NOT NULL);

-- Segredo do link que vai para o WhatsApp do dono ("recebi o Pix" / "nao recebi"). Guarda so o
-- SHA-256 do token: um dump do banco nao entrega links que confirmam pagamentos. E um segredo
-- DIFERENTE do codigo_publico, que o cliente conhece - senao o cliente confirmaria o proprio pagamento.
ALTER TABLE agendamentos ADD COLUMN token_confirmacao_hash VARCHAR(64);
CREATE UNIQUE INDEX uq_agendamentos_token_confirmacao ON agendamentos (token_confirmacao_hash)
    WHERE token_confirmacao_hash IS NOT NULL;
