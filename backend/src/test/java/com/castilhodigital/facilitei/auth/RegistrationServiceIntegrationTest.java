package com.castilhodigital.facilitei.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.billing.Plano;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.support.PostgresIntegrationTest;
import com.castilhodigital.facilitei.tenant.TenantRepository;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Cadastro do negocio contra o Postgres real, agora com o WhatsApp do dono (para onde vao as confirmacoes de Pix). */
class RegistrationServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private TenantRepository tenantRepository;

    private RegistrarTenantRequest cadastro(String slug, String telefoneDono) {
        return new RegistrarTenantRequest("Barbearia do Zé", slug, LocalTime.of(9, 0), LocalTime.of(18, 0),
                slug + "@example.com", "senha12345", "52998224725", Plano.BASICO, telefoneDono);
    }

    @Test
    void guardaOTelefoneDoDonoNoFormatoDoWhatsApp() {
        var resposta = registrationService.registrarTenantEAdmin(cadastro("com-telefone", "(63) 99999-9999"));

        assertThat(tenantRepository.findById(resposta.tenantId()).orElseThrow().getTelefoneDono()).isEqualTo("5563999999999");
    }

    @Test
    void telefoneEOpcionalNoCadastro() {
        var semTelefone = registrationService.registrarTenantEAdmin(cadastro("sem-telefone", null));
        var telefoneEmBranco = registrationService.registrarTenantEAdmin(cadastro("telefone-em-branco", "  "));

        assertThat(tenantRepository.findById(semTelefone.tenantId()).orElseThrow().getTelefoneDono()).isNull();
        assertThat(tenantRepository.findById(telefoneEmBranco.tenantId()).orElseThrow().getTelefoneDono()).isNull();
    }

    /** O cadastro e uma transacao so: telefone invalido nao pode deixar um negocio pela metade. */
    @Test
    void telefoneInvalidoRecusaOCadastroInteiro() {
        assertThatThrownBy(() -> registrationService.registrarTenantEAdmin(cadastro("telefone-ruim", "12345")))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThat(tenantRepository.existsBySlug("telefone-ruim")).isFalse();
    }

}
