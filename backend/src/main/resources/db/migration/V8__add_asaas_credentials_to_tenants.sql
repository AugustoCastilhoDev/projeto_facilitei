-- Modelo "traga sua propria conta de pagamento" (BYOPP): a cobranca Pix do
-- sinal passa a ser criada na propria conta Asaas do tenant, nao na da
-- plataforma. Os valores ficam cifrados em repouso pela aplicacao (ver
-- common.crypto.EncryptedStringConverter) - por isso TEXT, nao VARCHAR com
-- tamanho fixo (o texto cifrado e maior que o original).
ALTER TABLE tenants ADD COLUMN asaas_api_key TEXT;
ALTER TABLE tenants ADD COLUMN asaas_webhook_token TEXT;
