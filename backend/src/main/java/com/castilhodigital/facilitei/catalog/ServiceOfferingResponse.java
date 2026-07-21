package com.castilhodigital.facilitei.catalog;

import java.math.BigDecimal;

public record ServiceOfferingResponse(
        Long id,
        String nome,
        Integer duracaoMin,
        BigDecimal preco,
        BigDecimal sinalPercentual,
        boolean ativo
) {

    public static ServiceOfferingResponse from(ServiceOffering service) {
        return new ServiceOfferingResponse(
                service.getId(),
                service.getNome(),
                service.getDuracaoMin(),
                service.getPreco(),
                service.getSinalPercentual(),
                service.isAtivo());
    }

}
