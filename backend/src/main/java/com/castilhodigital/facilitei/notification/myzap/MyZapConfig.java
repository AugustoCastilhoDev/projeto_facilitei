package com.castilhodigital.facilitei.notification.myzap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP dedicado ao MyZap. Diferente da Asaas (BYOPP, chave por
 * tenant), o WhatsApp e enviado a partir de uma unica conta/numero da
 * propria plataforma - entao a chave fixa e um header default do
 * RestClient, nao precisa ser passada por chamada.
 *
 * So existe quando facilitei.notification.provider=myzap (ver
 * MyZapNotificationService) - nos demais casos (default: console) nem faz
 * sentido montar um client HTTP para um provedor que nao vai ser usado.
 */
@Configuration
@EnableConfigurationProperties(MyZapProperties.class)
@ConditionalOnProperty(prefix = "facilitei.notification", name = "provider", havingValue = "myzap")
@RequiredArgsConstructor
public class MyZapConfig {

    private final MyZapProperties myZapProperties;

    @Bean
    public RestClient myZapRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(myZapProperties.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + myZapProperties.apiKey())
                .build();
    }

}
