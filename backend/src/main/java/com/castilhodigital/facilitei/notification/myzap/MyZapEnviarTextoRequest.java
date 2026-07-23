package com.castilhodigital.facilitei.notification.myzap;

/** Corpo de POST /mensagens/texto - nomes de campo batem com o schema EnviarTexto da API do MyZap. */
public record MyZapEnviarTextoRequest(String numero, String texto) {
}
