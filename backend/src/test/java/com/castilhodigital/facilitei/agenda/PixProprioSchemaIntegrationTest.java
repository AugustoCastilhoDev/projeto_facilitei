package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import com.castilhodigital.facilitei.pix.TipoChavePix;
import com.castilhodigital.facilitei.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/** As regras que o V17 poe no PROPRIO banco: configuracao de Pix pela metade e agendamento em estado invalido nao existem. */
class PixProprioSchemaIntegrationTest extends CenarioBarbeariaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant comPix(String slug, TipoChavePix tipo, String chave, String nome, String cidade) {
        Tenant tenant = new Tenant();
        tenant.setNome("Negocio " + slug);
        tenant.setSlug(slug);
        tenant.setPixTipoChave(tipo);
        tenant.setPixChave(chave);
        tenant.setPixNomeRecebedor(nome);
        tenant.setPixCidadeRecebedor(cidade);
        return tenant;
    }

    @Test
    void chavePixCompletaOuNenhumaPixAceitaAConfiguracaoInteiraEASemNada() {
        assertThatCode(() -> tenantRepository.saveAndFlush(comPix("completo", TipoChavePix.CPF, "52998224725", "ZE", "PALMAS")))
                .doesNotThrowAnyException();
        assertThatCode(() -> tenantRepository.saveAndFlush(comPix("sem-pix", null, null, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void configuracaoDePixPelaMetadeERecusadaPeloBanco() {
        assertThatThrownBy(() -> tenantRepository.saveAndFlush(comPix("sem-nome", TipoChavePix.CPF, "52998224725", null, "PALMAS")))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_tenants_pix_completo");
        assertThatThrownBy(() -> tenantRepository.saveAndFlush(comPix("sem-tipo", null, "52998224725", "ZE", "PALMAS")))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_tenants_pix_completo");
        assertThatThrownBy(() -> tenantRepository.saveAndFlush(comPix("so-nome", null, null, "ZE", null)))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_tenants_pix_completo");
    }

    /** Aguardar a conferencia do dono sem o cliente ter avisado que pagou nao faz sentido (e esconderia um bug). */
    @Test
    void aguardandoConfirmacaoSemOAvisoDoClienteERecusadoPeloBanco() {
        Agendamento sem = novoAgendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        sem.setPagamentoInformadoEm(null);

        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(sem))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_agendamentos_confirmacao_tem_aviso");

        Agendamento com = novoAgendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        assertThatCode(() -> agendamentoRepository.saveAndFlush(com)).doesNotThrowAnyException();
    }

    @Test
    void oTokenDoLinkDoDonoIdentificaUmSoAgendamento() {
        Agendamento primeiro = novoAgendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.CONFIRMADO);
        primeiro.setTokenConfirmacaoHash("a".repeat(64));
        agendamentoRepository.saveAndFlush(primeiro);
        Agendamento segundo = novoAgendamento(negocio, ana, sp(11, 0), sp(11, 30), AgendamentoStatus.CONFIRMADO);
        segundo.setTokenConfirmacaoHash("a".repeat(64));

        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(segundo))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_agendamentos_token_confirmacao");
    }

    @Test
    void meioDeCobrancaDesconhecidoERecusado() {
        Agendamento agendamento = novoAgendamento(negocio, ana, sp(10, 0), sp(10, 30), AgendamentoStatus.CONFIRMADO);
        agendamento.setMeioCobranca(MeioCobranca.PIX_PROPRIO);
        Long id = agendamentoRepository.saveAndFlush(agendamento).getId();

        assertThat(jdbcTemplate.queryForObject("SELECT meio_cobranca FROM agendamentos WHERE id = ?", String.class, id))
                .isEqualTo("PIX_PROPRIO");
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE agendamentos SET meio_cobranca = 'BOLETO' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_agendamentos_meio_cobranca");
    }

}
