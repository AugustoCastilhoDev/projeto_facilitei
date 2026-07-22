package com.castilhodigital.facilitei.tenant;

import jakarta.validation.constraints.NotBlank;

public record AtualizarAsaasApiKeyRequest(@NotBlank(message = "Chave da API Asaas e obrigatoria.") String apiKey) {
}
