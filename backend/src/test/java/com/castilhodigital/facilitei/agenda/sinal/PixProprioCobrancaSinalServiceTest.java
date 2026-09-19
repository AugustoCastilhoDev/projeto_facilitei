package com.castilhodigital.facilitei.agenda.sinal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.castilhodigital.facilitei.pix.TipoChavePix;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PixProprioCobrancaSinalServiceTest {

    private final PixProprioCobrancaSinalService servico = new PixProprioCobrancaSinalService();

    private Tenant negocioComPix() {
        Tenant tenant = new Tenant();
        tenant.setPixTipoChave(TipoChavePix.TELEFONE);
        tenant.setPixChave("+5563988887777");
        tenant.setPixNomeRecebedor("BARBEARIA DO ZE");
        tenant.setPixCidadeRecebedor("PALMAS");
        return tenant;
    }

    @Test
    void atendeSoQuemTemChavePixCadastrada() {
        assertThat(servico.atende(negocioComPix())).isTrue();
        assertThat(servico.atende(new Tenant())).isFalse();

        Tenant chaveEmBranco = new Tenant();
        chaveEmBranco.setPixChave("  ");
        assertThat(servico.atende(chaveEmBranco)).isFalse();
    }

    @Test
    void confirmacaoEManual() {
        assertThat(servico.meio()).isEqualTo(MeioCobranca.PIX_PROPRIO);
        assertThat(servico.meio().confirmacaoManual()).isTrue();
    }

    @Test
    void criaOCopiaEColaComAChaveDoDonoOValorEOIdentificadorDoAgendamento() {
        CobrancaSinal cobranca = servico.criar(negocioComPix(),
                new SolicitacaoCobranca(42L, "João", "5563999999999", null, new BigDecimal("15.00")));

        assertThat(cobranca.payload())
                .startsWith("000201")
                .contains("br.gov.bcb.pix0114+5563988887777")
                .contains("540515.00")
                .contains("5915BARBEARIA DO ZE")
                .contains("6006PALMAS")
                .contains("0505FAC42");
        assertThat(cobranca.cobrancaExternaId()).as("Pix estatico nao tem cobranca para consultar").isNull();
        assertThat(cobranca.qrCodeBase64()).as("a tela desenha o QR a partir do copia e cola").isNull();
    }

    @Test
    void naoExigeNadaDoClienteNemTemComoRebuscarOuCancelar() {
        assertThatCode(() -> servico.validar(new SolicitacaoCobranca(null, "João", "5563999999999", null, BigDecimal.TEN)))
                .doesNotThrowAnyException();
        assertThat(servico.buscarQrCodeBase64(negocioComPix(), null)).isEmpty();
        assertThatCode(() -> servico.cancelar(negocioComPix(), null)).doesNotThrowAnyException();
    }

}
