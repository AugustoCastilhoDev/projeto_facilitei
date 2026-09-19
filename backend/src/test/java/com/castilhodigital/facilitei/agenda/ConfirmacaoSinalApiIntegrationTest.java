package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.JwtService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.user.UserService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

/**
 * O Pix proprio pela API HTTP: o cliente avisa que pagou (rota publica), o dono decide pelo link do WhatsApp (rota
 * publica, autorizada so pelo segredo do link) ou pelo painel (JWT). Cobre rotas, serializacao, codigos HTTP e isolamento.
 */
class ConfirmacaoSinalApiIntegrationTest extends CenarioPixProprioIntegrationTest {

    private static final String PUBLICO = "/api/public/tenants/barbearia/agenda";
    private static final String CONFIRMACOES = "/api/public/confirmacoes-sinal/";
    private static final String SEGUNDA_10H = "2026-09-21T10:00:00-03:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AgendamentoService agendamentoService;

    private String tokenDoDono;
    private String tokenDoConcorrente;

    @BeforeEach
    void criarDonos() {
        Tenant concorrente = tenant("concorrente");
        tokenDoDono = jwtService.gerarToken(userService.registrarAdmin(negocio, "dono@barbearia.com", "senha12345"));
        tokenDoConcorrente = jwtService.gerarToken(userService.registrarAdmin(concorrente, "dono@concorrente.com", "senha12345"));
    }

    private ResultActions reservar(String telefone) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("servicoIds", List.of(corte.getId()));
        corpo.put("profissionalId", ana.getId());
        corpo.put("inicio", SEGUNDA_10H);
        corpo.put("clienteNome", "João");
        corpo.put("clienteTelefone", telefone);
        return mockMvc.perform(post(PUBLICO + "/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    private String codigoDe(ResultActions resposta) throws Exception {
        return objectMapper.readTree(resposta.andReturn().getResponse().getContentAsString()).get("codigo").asString();
    }

    private ResultActions informarPagamento(String codigo) throws Exception {
        return mockMvc.perform(post(PUBLICO + "/agendamentos/" + codigo + "/informar-pagamento"));
    }

    /** Reserva, o cliente avisa que pagou e devolve o token que chegou ao WhatsApp do dono. */
    private String reservaAvisadaEToken() throws Exception {
        informarPagamento(codigoDe(reservar(TELEFONE_DIGITADO))).andExpect(status().isOk());
        ArgumentCaptor<String> mensagem = ArgumentCaptor.forClass(String.class);
        verify(notificationService).enviar(eq(TELEFONE_DONO), mensagem.capture());
        return tokenDaMensagem(mensagem.getValue());
    }

    // ================= o cliente avisa que pagou =================

    @Test
    void reservaNoPixProprioAvisaAoFrontQueAConfirmacaoEManual() throws Exception {
        reservar(TELEFONE_DIGITADO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.confirmacaoManual").value(true))
                .andExpect(jsonPath("$.pix.copiaECola").value(containsString("br.gov.bcb.pix0114" + CHAVE)))
                .andExpect(jsonPath("$.pix.qrCodeBase64").isEmpty());
    }

    @Test
    void jaPagueiSeguraOHorarioEDevolveOEstadoSemPixNemDadosDoCliente() throws Exception {
        String codigo = codigoDe(reservar(TELEFONE_DIGITADO));

        informarPagamento(codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                .andExpect(jsonPath("$.confirmacaoManual").value(true))
                .andExpect(jsonPath("$.expiraEm").value("2026-09-20T13:00:00-03:00"))   // agora (12:00) + 60 min
                .andExpect(jsonPath("$.pix").isEmpty())
                .andExpect(jsonPath("$.clienteTelefone").doesNotExist())
                .andExpect(jsonPath("$.tokenConfirmacao").doesNotExist());

        // repetir e seguro
        informarPagamento(codigo).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"));
        // e o polling da tela enxerga o novo estado
        mockMvc.perform(get(PUBLICO + "/agendamentos/" + codigo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"));
    }

    @Test
    void jaPagueiDeReservaInexistenteOuDeOutroNegocioRetorna404() throws Exception {
        String codigo = codigoDe(reservar(TELEFONE_DIGITADO));

        informarPagamento(UUID.randomUUID().toString()).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/public/tenants/concorrente/agenda/agendamentos/" + codigo + "/informar-pagamento"))
                .andExpect(status().isNotFound());
    }

    /** Pela Asaas quem confirma e o webhook; "ja paguei" ali so serviria para tentar confirmar sem pagar. */
    @Test
    void jaPagueiEmReservaDaAsaasRetorna400() throws Exception {
        Tenant atual = tenantRepository.findById(negocio.getId()).orElseThrow();
        atual.setAsaasApiKey("chave-asaas");   // a Asaas tem precedencia sobre o Pix proprio
        tenantRepository.saveAndFlush(atual);
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("servicoIds", List.of(corte.getId()));
        corpo.put("profissionalId", ana.getId());
        corpo.put("inicio", SEGUNDA_10H);
        corpo.put("clienteNome", "João");
        corpo.put("clienteTelefone", TELEFONE_DIGITADO);
        corpo.put("clienteCpfCnpj", CPF_VALIDO);
        ResultActions criada = mockMvc.perform(post(PUBLICO + "/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confirmacaoManual").value(false));

        informarPagamento(codigoDe(criada))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("automaticamente")));
    }

    @Test
    void jaPagueiDepoisDeExpirarComHorarioOcupadoRetorna409() throws Exception {
        String codigo = codigoDe(reservar(TELEFONE_DIGITADO));
        Instant depoisDoPrazo = AGORA.plus(Duration.ofMinutes(20));
        agendamentoService.expirarVencidos(depoisDoPrazo);
        agendamentoService.criar(new NovoAgendamento(negocio.getId(), List.of(corte.getId()), ana.getId(), sp(10, 0),
                "Carlos", "63955554444", null, null, AgendamentoOrigem.PUBLICO), depoisDoPrazo);

        // o relogio fixo dos controllers e AGORA (dentro do prazo original), entao a expiracao vem do job acima
        informarPagamento(codigo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("fale com o estabelecimento")));
    }

    // ================= o link do dono =================

    @Test
    void linkDoDonoMostraOQueEstaSendoConfirmadoSemPrecisarDeLogin() throws Exception {
        String token = reservaAvisadaEToken();

        mockMvc.perform(get(CONFIRMACOES + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                .andExpect(jsonPath("$.negocioNome").value("Barbearia barbearia"))
                .andExpect(jsonPath("$.referencia").value(containsString("FAC")))
                .andExpect(jsonPath("$.clienteNome").value("João"))
                .andExpect(jsonPath("$.inicio").value(SEGUNDA_10H))
                .andExpect(jsonPath("$.profissionalNome").value("Ana"))
                .andExpect(jsonPath("$.servicos[0].nome").value("Corte"))
                .andExpect(jsonPath("$.valorSinal").value(15.0))
                .andExpect(jsonPath("$.podeConfirmar").value(true))
                .andExpect(jsonPath("$.podeRecusar").value(true))
                .andExpect(jsonPath("$.clienteTelefone").doesNotExist())
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void recebiConfirmaOHorarioEEIdempotente() throws Exception {
        String token = reservaAvisadaEToken();

        mockMvc.perform(post(CONFIRMACOES + token + "/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.podeRecusar").value(false));
        mockMvc.perform(post(CONFIRMACOES + token + "/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
        // o cliente enxerga a confirmacao no polling da tela dele
        mockMvc.perform(get(PUBLICO + "/agendamentos/" + agendamentoRepository.findAll().get(0).getCodigoPublico()))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    void naoRecebiCancelaEDevolveOHorarioParaAAgenda() throws Exception {
        String token = reservaAvisadaEToken();

        mockMvc.perform(post(CONFIRMACOES + token + "/recusar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO_NEGOCIO"))
                .andExpect(jsonPath("$.podeConfirmar").value(false))
                .andExpect(jsonPath("$.podeRecusar").value(false));

        // o mesmo horario aparece livre de novo na disponibilidade
        mockMvc.perform(get(PUBLICO + "/disponibilidade").param("servicoIds", String.valueOf(corte.getId()))
                        .param("profissionalId", String.valueOf(ana.getId())).param("data", "2026-09-21"))
                .andExpect(jsonPath("$[?(@.inicio == '" + SEGUNDA_10H + "')]").isNotEmpty());
    }

    @Test
    void confirmarUmaReservaJaRecusadaRetorna400() throws Exception {
        String token = reservaAvisadaEToken();
        mockMvc.perform(post(CONFIRMACOES + token + "/recusar")).andExpect(status().isOk());

        mockMvc.perform(post(CONFIRMACOES + token + "/confirmar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("cancelado")));
    }

    /** O 404 e igual para "nunca existiu", "formato errado" e "codigo publico do cliente": nao ensina nada a quem tenta adivinhar. */
    @Test
    void tokenInvalidoRetorna404IgualParaTodosOsCasos() throws Exception {
        String token = reservaAvisadaEToken();
        String codigoDoCliente = agendamentoRepository.findAll().get(0).getCodigoPublico().toString();

        for (String invalido : List.of("x".repeat(43), codigoDoCliente, "curto", "a".repeat(200))) {
            mockMvc.perform(get(CONFIRMACOES + invalido)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(containsString("Link inválido")));
            mockMvc.perform(post(CONFIRMACOES + invalido + "/confirmar")).andExpect(status().isNotFound());
            mockMvc.perform(post(CONFIRMACOES + invalido + "/recusar")).andExpect(status().isNotFound());
        }
        assertThat(agendamentoRepository.findAll().get(0).getStatus()).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        mockMvc.perform(get(CONFIRMACOES + token)).andExpect(status().isOk());
    }

    @Test
    void confirmarDepoisDeExpirarComHorarioOcupadoRetorna409ComOOrientacaoDeDevolver() throws Exception {
        String token = reservaAvisadaEToken();
        Instant depoisDoPrazo = AGORA.plus(Duration.ofMinutes(90));
        agendamentoService.expirarVencidos(depoisDoPrazo);
        agendamentoService.criar(new NovoAgendamento(negocio.getId(), List.of(corte.getId()), ana.getId(), sp(10, 0),
                "Carlos", "63955554444", null, null, AgendamentoOrigem.PUBLICO), depoisDoPrazo);

        mockMvc.perform(post(CONFIRMACOES + token + "/confirmar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Devolva o sinal ao cliente")));
    }

    // ================= painel =================

    @Test
    void painelMostraQuemAvisouQuePagouComAReferenciaParaConferirNoExtrato() throws Exception {
        reservaAvisadaEToken();

        mockMvc.perform(get("/api/admin/tenants/" + negocio.getId() + "/agendamentos").param("de", "2026-09-21")
                        .header("Authorization", "Bearer " + tokenDoDono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AGUARDANDO_CONFIRMACAO"))
                .andExpect(jsonPath("$[0].pagamentoInformado").value(true))
                .andExpect(jsonPath("$[0].sinalConfirmado").value(false))
                .andExpect(jsonPath("$[0].meioCobranca").value("PIX_PROPRIO"))
                .andExpect(jsonPath("$[0].referenciaPix").value(containsString("FAC")));
    }

    @Test
    void painelRecusaOPagamentoEODonoDeOutroNegocioNaoPode() throws Exception {
        reservaAvisadaEToken();
        Long id = agendamentoRepository.findAll().get(0).getId();
        String caminho = "/api/admin/tenants/" + negocio.getId() + "/agendamentos/" + id + "/recusar-pagamento";

        mockMvc.perform(post(caminho)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(caminho).header("Authorization", "Bearer " + tokenDoConcorrente)).andExpect(status().isForbidden());
        assertThat(agendamentoRepository.findById(id).orElseThrow().getStatus()).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);

        mockMvc.perform(post(caminho).header("Authorization", "Bearer " + tokenDoDono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO_NEGOCIO"));
    }

    @Test
    void painelConfirmaOPagamentoDeQuemAvisou() throws Exception {
        reservaAvisadaEToken();
        Long id = agendamentoRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/admin/tenants/" + negocio.getId() + "/agendamentos/" + id + "/confirmar-pagamento")
                        .header("Authorization", "Bearer " + tokenDoDono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.sinalConfirmado").value(true));
    }

}
