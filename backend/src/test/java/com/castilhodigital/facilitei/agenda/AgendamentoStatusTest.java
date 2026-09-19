package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgendamentoStatusTest {

    @Test
    void ocupaHorarioSoParaOsStatusQueRealmenteSegurameAAgenda() {
        assertThat(AgendamentoStatus.QUE_OCUPAM_HORARIO).containsExactlyInAnyOrder(
                AgendamentoStatus.AGUARDANDO_PAGAMENTO, AgendamentoStatus.AGUARDANDO_CONFIRMACAO,
                AgendamentoStatus.CONFIRMADO, AgendamentoStatus.CONCLUIDO, AgendamentoStatus.FALTOU);
    }

    @Test
    void soOsStatusDeEsperaExigemPrazo() {
        assertThat(AgendamentoStatus.AGUARDANDO_PAGAMENTO.aguardaSinal()).isTrue();
        assertThat(AgendamentoStatus.AGUARDANDO_CONFIRMACAO.aguardaSinal()).isTrue();
        assertThat(AgendamentoStatus.CONFIRMADO.aguardaSinal()).isFalse();
        assertThat(AgendamentoStatus.EXPIRADO.aguardaSinal()).isFalse();
    }

}
