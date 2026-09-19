package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.JwtService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.user.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

/** Configuracao do recebimento (Pix proprio e WhatsApp do dono) pela API do painel, e o cadastro com o telefone do dono. */
class RecebimentoApiIntegrationTest extends CenarioBarbeariaIntegrationTest {

    private static final String PUBLICO = "/api/public/tenants/barbearia/agenda";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    private String tokenDoDono;
    private String tokenDoConcorrente;

    @BeforeEach
    void criarDonos() {
        Tenant concorrente = tenant("concorrente");
        tokenDoDono = jwtService.gerarToken(userService.registrarAdmin(negocio, "dono@barbearia.com", "senha12345"));
        tokenDoConcorrente = jwtService.gerarToken(userService.registrarAdmin(concorrente, "dono@concorrente.com", "senha12345"));
    }

    private String admin(String caminho) {
        return "/api/admin/tenants/" + negocio.getId() + caminho;
    }

    private ResultActions putAdmin(String caminho, String token, Object corpo) throws Exception {
        return mockMvc.perform(put(admin(caminho)).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo)));
    }

    private ResultActions getRecebimento(String token) throws Exception {
        return mockMvc.perform(get(admin("/recebimento")).header("Authorization", "Bearer " + token));
    }

    private Map<String, Object> chavePix(String tipo, String chave) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("tipoChave", tipo);
        corpo.put("chave", chave);
        return corpo;
    }

    private void cadastrarTelefoneDono() throws Exception {
        putAdmin("/telefone-dono", tokenDoDono, Map.of("telefone", "(63) 97777-6666")).andExpect(status().isOk());
    }

    // ---- consulta ----

    @Test
    void negocioSoComAsaasAparecePelaAsaasSemPixNemTelefone() throws Exception {
        getRecebimento(tokenDoDono)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meioAtivo").value("ASAAS"))
                .andExpect(jsonPath("$.asaasConfigurado").value(true))
                .andExpect(jsonPath("$.pixProprio").isEmpty())
                .andExpect(jsonPath("$.telefoneDono").isEmpty());
    }

    @Test
    void negocioSemNenhumMeioNaoTemMeioAtivo() throws Exception {
        negocio.setAsaasApiKey(null);
        tenantRepository.saveAndFlush(negocio);

        getRecebimento(tokenDoDono)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meioAtivo").isEmpty())
                .andExpect(jsonPath("$.asaasConfigurado").value(false));
    }

    // ---- WhatsApp do dono ----

    @Test
    void telefoneDoDonoEGuardadoNoFormatoDoWhatsApp() throws Exception {
        putAdmin("/telefone-dono", tokenDoDono, Map.of("telefone", "(63) 97777-6666"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefoneDono").value("5563977776666"));

        assertThat(tenantRepository.findById(negocio.getId()).orElseThrow().getTelefoneDono()).isEqualTo("5563977776666");
    }

    @Test
    void telefoneInvalidoOuEmBrancoRetorna400() throws Exception {
        putAdmin("/telefone-dono", tokenDoDono, Map.of("telefone", "12345")).andExpect(status().isBadRequest());
        putAdmin("/telefone-dono", tokenDoDono, Map.of("telefone", " ")).andExpect(status().isBadRequest());
    }

    // ---- Pix proprio ----

    /** Sem o WhatsApp do dono ninguem o chamaria para confirmar: o Pix proprio nao pode ser ativado antes. */
    @Test
    void pixProprioExigeOWhatsAppDoDonoJaCadastrado() throws Exception {
        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("TELEFONE", "(63) 98888-7777"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("WhatsApp do dono")));

        assertThat(tenantRepository.findById(negocio.getId()).orElseThrow().getPixChave()).isNull();
    }

    @Test
    void configuraAChaveNormalizadaENomeECidadePadrao() throws Exception {
        cadastrarTelefoneDono();

        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("TELEFONE", "(63) 98888-7777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pixProprio.tipoChave").value("TELEFONE"))
                .andExpect(jsonPath("$.pixProprio.chave").value("+5563988887777"))
                .andExpect(jsonPath("$.pixProprio.nomeRecebedor").value("BARBEARIA BARBEARIA"))
                .andExpect(jsonPath("$.pixProprio.cidadeRecebedor").value("BRASIL"));
    }

    @Test
    void nomeECidadeInformadosVaoSemAcentoEmMaiusculas() throws Exception {
        cadastrarTelefoneDono();
        Map<String, Object> corpo = chavePix("EMAIL", "Dono@Barbearia.com.br");
        corpo.put("nomeRecebedor", "Barbearia do Zé");
        corpo.put("cidadeRecebedor", "São Luís");

        putAdmin("/recebimento/pix-proprio", tokenDoDono, corpo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pixProprio.chave").value("dono@barbearia.com.br"))
                .andExpect(jsonPath("$.pixProprio.nomeRecebedor").value("BARBEARIA DO ZE"))
                .andExpect(jsonPath("$.pixProprio.cidadeRecebedor").value("SAO LUIS"));
    }

    @Test
    void chaveInvalidaOuSemTipoRetorna400() throws Exception {
        cadastrarTelefoneDono();

        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("CPF", "529.982.247-26")).andExpect(status().isBadRequest());
        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix(null, "529.982.247-25")).andExpect(status().isBadRequest());
        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("CPF", " ")).andExpect(status().isBadRequest());
        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("QUALQUER", "x")).andExpect(status().isBadRequest());

        assertThat(tenantRepository.findById(negocio.getId()).orElseThrow().getPixChave()).isNull();
    }

    @Test
    void asaasTemPrecedenciaEQuandoSaiOPixProprioAssume() throws Exception {
        cadastrarTelefoneDono();
        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("CPF", "529.982.247-25")).andExpect(status().isOk())
                .andExpect(jsonPath("$.meioAtivo").value("ASAAS"));

        // recarrega: o "negocio" do cenario e anterior a chave Pix e a sobrescreveria ao salvar
        Tenant atual = tenantRepository.findById(negocio.getId()).orElseThrow();
        atual.setAsaasApiKey(null);
        tenantRepository.saveAndFlush(atual);

        getRecebimento(tokenDoDono).andExpect(jsonPath("$.meioAtivo").value("PIX_PROPRIO"));
    }

    @Test
    void removerOPixProprioLimpaTudo() throws Exception {
        cadastrarTelefoneDono();
        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("CPF", "529.982.247-25")).andExpect(status().isOk());

        mockMvc.perform(delete(admin("/recebimento/pix-proprio")).header("Authorization", "Bearer " + tokenDoDono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pixProprio").isEmpty());

        Tenant recarregado = tenantRepository.findById(negocio.getId()).orElseThrow();
        assertThat(recarregado.getPixChave()).isNull();
        assertThat(recarregado.getPixTipoChave()).isNull();
        assertThat(recarregado.getPixNomeRecebedor()).isNull();
        assertThat(recarregado.getPixCidadeRecebedor()).isNull();
        assertThat(recarregado.getTelefoneDono()).as("o WhatsApp do dono nao some junto").isEqualTo("5563977776666");
    }

    // ---- ponta a ponta: o cliente recebe o Pix do dono ----

    @Test
    void clienteRecebeOCopiaEColaComAChaveDoDonoEOQrSaiDoTexto() throws Exception {
        negocio.setAsaasApiKey(null);
        tenantRepository.saveAndFlush(negocio);
        cadastrarTelefoneDono();
        putAdmin("/recebimento/pix-proprio", tokenDoDono, chavePix("CPF", "529.982.247-25")).andExpect(status().isOk());

        Map<String, Object> pedido = new HashMap<>();
        pedido.put("servicoIds", List.of(corte.getId()));
        pedido.put("profissionalId", ana.getId());
        pedido.put("inicio", "2026-09-21T10:00:00-03:00");
        pedido.put("clienteNome", "João");
        pedido.put("clienteTelefone", TELEFONE_DIGITADO);
        mockMvc.perform(post(PUBLICO + "/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.pix.copiaECola").value(startsWith("000201")))
                .andExpect(jsonPath("$.pix.copiaECola").value(containsString("0111" + "52998224725")))
                .andExpect(jsonPath("$.pix.qrCodeBase64").isEmpty());
    }

    // ---- seguranca ----

    @Test
    void semTokenRetorna401EDeOutroNegocioRetorna403() throws Exception {
        mockMvc.perform(get(admin("/recebimento"))).andExpect(status().isUnauthorized());

        getRecebimento(tokenDoConcorrente).andExpect(status().isForbidden());
        putAdmin("/telefone-dono", tokenDoConcorrente, Map.of("telefone", "63977776666")).andExpect(status().isForbidden());
        putAdmin("/recebimento/pix-proprio", tokenDoConcorrente, chavePix("CPF", "529.982.247-25")).andExpect(status().isForbidden());
        mockMvc.perform(delete(admin("/recebimento/pix-proprio")).header("Authorization", "Bearer " + tokenDoConcorrente))
                .andExpect(status().isForbidden());

        Tenant intacto = tenantRepository.findById(negocio.getId()).orElseThrow();
        assertThat(intacto.getTelefoneDono()).isNull();
        assertThat(intacto.getPixChave()).isNull();
    }

}
