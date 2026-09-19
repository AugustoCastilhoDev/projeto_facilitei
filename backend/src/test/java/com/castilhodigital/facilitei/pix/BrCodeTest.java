package com.castilhodigital.facilitei.pix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BrCodeTest {

    private static final String CHAVE = "123e4567-e12b-12d1-a456-426655440000";

    /** Vetor de teste publico do CRC-16/CCITT-FALSE: qualquer implementacao correta devolve 29B1 para "123456789". */
    @Test
    void crcBateComOVetorPublicoDoAlgoritmo() {
        assertThat(BrCode.crc16("123456789")).isEqualTo("29B1");
    }

    /** Exemplo do manual de BR Code do Banco Central (chave aleatoria, sem valor, txid "***"): o CRC e o publicado la. */
    @Test
    void crcBateComOExemploDoManualDoBancoCentral() {
        String semCrc = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000"
                + "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304";

        assertThat(BrCode.crc16(semCrc)).isEqualTo("1D3D");
    }

    @Test
    void geraOsCamposDoPixNaOrdemDoPadraoComOValorEOTxid() {
        String payload = BrCode.gerar(CHAVE, "BARBEARIA DO ZE", "PALMAS", new BigDecimal("15"), "FAC42");

        Map<String, String> campos = campos(payload);
        assertThat(campos.keySet()).containsExactly("00", "01", "26", "52", "53", "54", "58", "59", "60", "62", "63");
        assertThat(campos).containsEntry("00", "01")
                .containsEntry("53", "986")
                .containsEntry("54", "15.00")
                .containsEntry("58", "BR")
                .containsEntry("59", "BARBEARIA DO ZE")
                .containsEntry("60", "PALMAS");
        assertThat(campos(campos.get("26"))).containsEntry("00", "br.gov.bcb.pix").containsEntry("01", CHAVE);
        assertThat(campos(campos.get("62"))).containsEntry("05", "FAC42");
    }

    /** O CRC cobre o texto todo ate "6304": qualquer alteracao (valor, chave) exigiria recalcula-lo. */
    @Test
    void crcDoPayloadGeradoConfere() {
        String payload = BrCode.gerar(CHAVE, "BARBEARIA DO ZE", "PALMAS", new BigDecimal("15.00"), "FAC42");

        int fimSemCrc = payload.length() - 4;
        assertThat(payload.substring(fimSemCrc - 4, fimSemCrc)).isEqualTo("6304");
        assertThat(payload.substring(fimSemCrc)).isEqualTo(BrCode.crc16(payload.substring(0, fimSemCrc)));
    }

    @Test
    void valorSempreComDuasCasasEPontoDecimal() {
        assertThat(campos(BrCode.gerar(CHAVE, "A", "B", new BigDecimal("7.5"), "X1")).get("54")).isEqualTo("7.50");
        assertThat(campos(BrCode.gerar(CHAVE, "A", "B", new BigDecimal("1234.567"), "X1")).get("54")).isEqualTo("1234.57");
    }

    /** O tamanho de cada campo e o dos CARACTERES, nao dos bytes: nome/cidade sao ASCII exatamente por isso. */
    @Test
    void aceitaTelefoneEEmailComoChave() {
        assertThat(campos(campos(BrCode.gerar("+5563999999999", "A", "B", BigDecimal.TEN, "X1")).get("26")))
                .containsEntry("01", "+5563999999999");
        assertThat(campos(campos(BrCode.gerar("dono@barbearia.com.br", "A", "B", BigDecimal.TEN, "X1")).get("26")))
                .containsEntry("01", "dono@barbearia.com.br");
    }

    @Test
    void recusaValorNaoPositivoTxidInvalidoENomeLongoDemais() {
        assertThatThrownBy(() -> BrCode.gerar(CHAVE, "A", "B", BigDecimal.ZERO, "X1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BrCode.gerar(CHAVE, "A", "B", new BigDecimal("-1"), "X1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BrCode.gerar(CHAVE, "A", "B", BigDecimal.TEN, "com espaco"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BrCode.gerar(CHAVE, "A", "B", BigDecimal.TEN, "A".repeat(26)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BrCode.gerar(CHAVE, "A".repeat(26), "B", BigDecimal.TEN, "X1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BrCode.gerar(CHAVE, "A", "B".repeat(16), BigDecimal.TEN, "X1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sanitizarTiraAcentoPontuacaoEExcessoEnCorta() {
        assertThat(BrCode.sanitizar("Barbearia do Zé & Filhos", 25)).isEqualTo("BARBEARIA DO ZE FILHOS");
        assertThat(BrCode.sanitizar("  São   Luís  ", 15)).isEqualTo("SAO LUIS");
        assertThat(BrCode.sanitizar("Salão de Beleza da Maria Aparecida", 25)).isEqualTo("SALAO DE BELEZA DA MARIA");
        assertThat(BrCode.sanitizar("Palmas do Tocantins", 15)).isEqualTo("PALMAS DO TOCAN");
        assertThat(BrCode.sanitizar("Rua Sao Paulo", 8)).as("sem espaco sobrando no fim do corte").isEqualTo("RUA SAO");
        assertThat(BrCode.sanitizar("***", 25)).isEmpty();
        assertThat(BrCode.sanitizar(null, 25)).isEmpty();
    }

    /** Le "ID(2) + tamanho(2) + valor" em sequencia - um parser independente do que gerou o texto. */
    private static Map<String, String> campos(String texto) {
        Map<String, String> resultado = new LinkedHashMap<>();
        int posicao = 0;
        while (posicao < texto.length()) {
            String id = texto.substring(posicao, posicao + 2);
            int tamanho = Integer.parseInt(texto.substring(posicao + 2, posicao + 4));
            resultado.put(id, texto.substring(posicao + 4, posicao + 4 + tamanho));
            posicao += 4 + tamanho;
        }
        return resultado;
    }

}
