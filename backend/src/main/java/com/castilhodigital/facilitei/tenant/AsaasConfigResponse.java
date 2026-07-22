package com.castilhodigital.facilitei.tenant;

/** Nunca inclui a propria API key - so o suficiente para o admin configurar o webhook do lado da Asaas. */
public record AsaasConfigResponse(boolean configurado, String webhookUrl, String webhookToken) {

    public static AsaasConfigResponse from(Tenant tenant, String webhookUrl) {
        boolean configurado = tenant.getAsaasApiKey() != null && !tenant.getAsaasApiKey().isBlank();
        return new AsaasConfigResponse(configurado, webhookUrl, tenant.getAsaasWebhookToken());
    }

}
