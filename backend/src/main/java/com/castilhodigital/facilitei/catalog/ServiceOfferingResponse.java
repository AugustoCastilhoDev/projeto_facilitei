package com.castilhodigital.facilitei.catalog;

import com.castilhodigital.facilitei.professional.Profissional;
import java.math.BigDecimal;
import java.util.List;

public record ServiceOfferingResponse(
        Long id,
        String nome,
        Integer duracaoMin,
        BigDecimal preco,
        BigDecimal sinalPercentual,
        boolean ativo,
        List<Long> profissionalIds,
        List<String> profissionalNomes
) {

    public static ServiceOfferingResponse from(ServiceOffering service, List<Profissional> vinculados) {
        List<Long> profissionalIds = vinculados.stream().map(Profissional::getId).sorted().toList();
        List<String> profissionalNomes = vinculados.stream().map(Profissional::getNome).sorted().toList();
        return new ServiceOfferingResponse(
                service.getId(),
                service.getNome(),
                service.getDuracaoMin(),
                service.getPreco(),
                service.getSinalPercentual(),
                service.isAtivo(),
                profissionalIds,
                profissionalNomes);
    }

}
