package com.castilhodigital.facilitei.agenda.sinal;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Escolhe o meio de cobranca do sinal. Para um agendamento NOVO vale a configuracao atual do negocio
 * (exigir/meioAtivo); para um agendamento que ja existe vale o meio com que ele nasceu (porMeio), mesmo
 * que o dono tenha trocado de meio desde entao.
 *
 * A ordem da lista e a de precedencia (@Order das implementacoes): quem configurou a Asaas E a chave Pix
 * fica com a Asaas, que confirma sozinha.
 */
@Component
@RequiredArgsConstructor
public class CobrancaSinalResolver {

    private final List<CobrancaSinalService> meiosDeCobranca;

    /**
     * @throws RegraDeNegocioException se o negocio ainda nao configurou nenhum meio de receber o sinal
     */
    public CobrancaSinalService exigir(Tenant tenant) {
        return meioAtivo(tenant).orElseThrow(() -> new RegraDeNegocioException(
                "Este negócio ainda não configurou o recebimento do sinal. Tente novamente mais tarde."));
    }

    public Optional<CobrancaSinalService> meioAtivo(Tenant tenant) {
        return meiosDeCobranca.stream().filter(meio -> meio.atende(tenant)).findFirst();
    }

    /** O meio de um agendamento que ja existe: independe da configuracao de hoje do negocio. */
    public CobrancaSinalService porMeio(MeioCobranca meio) {
        return meiosDeCobranca.stream()
                .filter(candidato -> candidato.meio() == meio)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma implementação de cobrança para o meio " + meio));
    }

}
