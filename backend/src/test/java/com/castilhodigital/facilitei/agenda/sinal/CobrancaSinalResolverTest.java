package com.castilhodigital.facilitei.agenda.sinal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CobrancaSinalResolverTest {

    private final AsaasCobrancaSinalService asaas = new AsaasCobrancaSinalService(mock(PaymentGatewayService.class));
    private final PixProprioCobrancaSinalService pixProprio = new PixProprioCobrancaSinalService();
    // a ordem da lista e a que o Spring monta pelos @Order (Asaas antes do Pix proprio)
    private final CobrancaSinalResolver resolver = new CobrancaSinalResolver(List.of(asaas, pixProprio));

    private Tenant negocio(boolean comAsaas, boolean comPix) {
        Tenant tenant = new Tenant();
        if (comAsaas) {
            tenant.setAsaasApiKey("chave-asaas");
        }
        if (comPix) {
            tenant.setPixChave("+5563988887777");
        }
        return tenant;
    }

    @Test
    void escolheOMeioQueOnegocioConfigurou() {
        assertThat(resolver.exigir(negocio(true, false)).meio()).isEqualTo(MeioCobranca.ASAAS);
        assertThat(resolver.exigir(negocio(false, true)).meio()).isEqualTo(MeioCobranca.PIX_PROPRIO);
    }

    /** Quem tem os dois fica com a confirmacao automatica da Asaas. */
    @Test
    void asaasTemPrecedenciaSobreOPixProprio() {
        assertThat(resolver.exigir(negocio(true, true)).meio()).isEqualTo(MeioCobranca.ASAAS);
    }

    @Test
    void semNenhumMeioConfiguradoRecusaComMensagemParaOCliente() {
        assertThatThrownBy(() -> resolver.exigir(negocio(false, false)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não configurou o recebimento do sinal");
        assertThat(resolver.meioAtivo(negocio(false, false))).isEmpty();
    }

    /** Uma reserva em andamento segue o meio com que nasceu, mesmo que o dono tenha trocado a configuracao. */
    @Test
    void porMeioIgnoraAConfiguracaoAtualDoNegocio() {
        assertThat(resolver.porMeio(MeioCobranca.PIX_PROPRIO)).isSameAs(pixProprio);
        assertThat(resolver.porMeio(MeioCobranca.ASAAS)).isSameAs(asaas);
    }

}
