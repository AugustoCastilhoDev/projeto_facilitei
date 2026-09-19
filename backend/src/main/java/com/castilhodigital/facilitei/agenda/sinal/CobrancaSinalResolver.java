package com.castilhodigital.facilitei.agenda.sinal;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Escolhe, para cada negocio, o meio de cobranca do sinal que ele configurou. */
@Component
@RequiredArgsConstructor
public class CobrancaSinalResolver {

    private final List<CobrancaSinalService> meiosDeCobranca;

    /**
     * @throws RegraDeNegocioException se o negocio ainda nao configurou nenhum meio de receber o sinal
     */
    public CobrancaSinalService exigir(Tenant tenant) {
        return meiosDeCobranca.stream()
                .filter(meio -> meio.atende(tenant))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Este negócio ainda não configurou o recebimento do sinal. Tente novamente mais tarde."));
    }

}
