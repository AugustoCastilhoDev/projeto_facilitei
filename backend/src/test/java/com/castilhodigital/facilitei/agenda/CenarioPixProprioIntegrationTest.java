package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.pix.TipoChavePix;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;

/**
 * O cenario da barbearia recebendo o sinal so na chave Pix do dono (sem Asaas), com o WhatsApp do dono cadastrado.
 * Ana faz Corte/Barba/Lavagem (Corte e Barba com 30% de sinal), na segunda 2026-09-21, "agora" = domingo 12:00.
 */
abstract class CenarioPixProprioIntegrationTest extends CenarioBarbeariaIntegrationTest {

    protected static final String CHAVE = "+5563988887777";
    protected static final String TELEFONE_DONO = "5563977776666";
    private static final Pattern TOKEN_NO_LINK = Pattern.compile("/confirmar-sinal/([A-Za-z0-9_-]{43})");

    @BeforeEach
    void trocarAsaasPorPixProprio() {
        negocio.setAsaasApiKey(null);
        negocio.setTelefoneDono(TELEFONE_DONO);
        negocio.setPixTipoChave(TipoChavePix.TELEFONE);
        negocio.setPixChave(CHAVE);
        negocio.setPixNomeRecebedor("BARBEARIA DO ZE");
        negocio.setPixCidadeRecebedor("PALMAS");
        negocio = tenantRepository.saveAndFlush(negocio);
    }

    /** O token que o dono recebeu, tirado da mensagem de WhatsApp (e a unica vez que ele existe em claro). */
    protected static String tokenDaMensagem(String mensagem) {
        Matcher achado = TOKEN_NO_LINK.matcher(mensagem);
        if (!achado.find()) {
            throw new AssertionError("Sem link de confirmacao na mensagem: " + mensagem);
        }
        return achado.group(1);
    }

    protected NovoAgendamento pedidoDoCorteAsDez() {
        return pedido(List.of(corte.getId()), ana.getId(), sp(10, 0));
    }

}
