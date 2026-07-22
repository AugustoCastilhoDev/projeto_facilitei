package com.castilhodigital.facilitei.payment.asaas;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP dedicado ao Asaas. A API do Asaas autentica via header
 * "access_token" (nao "Authorization: Bearer") - detalhe confirmado na
 * documentacao oficial (docs.asaas.com), diferente do padrao mais comum de
 * outras APIs REST.
 *
 * O access_token NAO e um header fixo aqui: cada tenant tem sua propria
 * conta Asaas (modelo BYOPP), entao a chave e passada por chamada em
 * AsaasClient, resolvida a partir do tenant dono do agendamento.
 */
@Configuration
@EnableConfigurationProperties(AsaasProperties.class)
@RequiredArgsConstructor
public class AsaasConfig {

    private final AsaasProperties asaasProperties;

    @Bean
    public RestClient asaasRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(asaasProperties.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "Facilitei-MVP")
                .build();
    }

}
