package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingRepository;
import com.castilhodigital.facilitei.notification.NotificationService;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalRepository;
import com.castilhodigital.facilitei.support.PostgresIntegrationTest;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Base dos testes da agenda com Postgres real: monta negocio, profissionais, servicos e agendamentos com poucas linhas. */
abstract class AgendaIntegrationTest extends PostgresIntegrationTest {

    /**
     * Os dois dubles vivem na base (nao em cada teste) para todos os testes da agenda compartilharem UM contexto do
     * Spring - cada combinacao diferente de @MockitoBean sobe a aplicacao de novo (~20s).
     */
    @MockitoBean
    protected NotificationService notificationService;

    @MockitoBean
    protected PaymentGatewayService paymentGatewayService;

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected ProfissionalRepository profissionalRepository;

    @Autowired
    protected ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    protected HorarioTrabalhoRepository horarioTrabalhoRepository;

    @Autowired
    protected BloqueioRepository bloqueioRepository;

    @Autowired
    protected AgendamentoRepository agendamentoRepository;

    protected Tenant tenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setNome("Barbearia " + slug);
        tenant.setSlug(slug);
        return tenantRepository.save(tenant);
    }

    protected ServiceOffering servico(Tenant tenant, String nome, int duracaoMin, String preco) {
        return servicoComSinal(tenant, nome, duracaoMin, preco, "0.00");
    }

    protected ServiceOffering servicoComSinal(Tenant tenant, String nome, int duracaoMin, String preco, String sinalPercentual) {
        ServiceOffering servico = new ServiceOffering();
        servico.setTenant(tenant);
        servico.setNome(nome);
        servico.setDuracaoMin(duracaoMin);
        servico.setPreco(new BigDecimal(preco));
        servico.setSinalPercentual(new BigDecimal(sinalPercentual));
        return serviceOfferingRepository.save(servico);
    }

    /** Profissional que realiza os servicos informados. As colunas de expediente legadas (agenda antiga) ainda sao obrigatorias no schema. */
    protected Profissional profissional(Tenant tenant, String nome, ServiceOffering... servicos) {
        Profissional profissional = new Profissional();
        profissional.setTenant(tenant);
        profissional.setNome(nome);
        profissional.setHorarioAbertura(LocalTime.of(9, 0));
        profissional.setHorarioFechamento(LocalTime.of(18, 0));
        profissional.setServicos(new HashSet<>(List.of(servicos)));
        return profissionalRepository.save(profissional);
    }

    protected HorarioTrabalho horario(Profissional profissional, DayOfWeek dia, LocalTime inicio, LocalTime fim) {
        HorarioTrabalho horario = new HorarioTrabalho();
        horario.setProfissional(profissional);
        horario.setDiaSemana(dia);
        horario.setInicio(inicio);
        horario.setFim(fim);
        return horarioTrabalhoRepository.saveAndFlush(horario);
    }

    protected Bloqueio bloqueio(Profissional profissional, Instant inicio, Instant fim) {
        Bloqueio bloqueio = new Bloqueio();
        bloqueio.setProfissional(profissional);
        bloqueio.setInicio(inicio);
        bloqueio.setFim(fim);
        bloqueio.setMotivo("Folga");
        return bloqueioRepository.saveAndFlush(bloqueio);
    }

    /** Monta (sem gravar) um agendamento valido; quem espera sinal ganha um prazo, como exige o banco. */
    protected Agendamento novoAgendamento(Tenant tenant, Profissional profissional, Instant inicio, Instant fim,
                                           AgendamentoStatus status) {
        Agendamento agendamento = new Agendamento();
        agendamento.setTenant(tenant);
        agendamento.setProfissional(profissional);
        agendamento.setClienteNome("Cliente Teste");
        agendamento.setClienteTelefone("5563999999999");
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setStatus(status);
        agendamento.setOrigem(AgendamentoOrigem.PUBLICO);
        agendamento.setValorTotal(new BigDecimal("50.00"));
        if (status.aguardaSinal()) {
            agendamento.setExpiraEm(inicio.minusSeconds(3600));
        }
        return agendamento;
    }

    protected Agendamento agendamento(Tenant tenant, Profissional profissional, Instant inicio, Instant fim,
                                       AgendamentoStatus status) {
        return agendamentoRepository.saveAndFlush(novoAgendamento(tenant, profissional, inicio, fim, status));
    }

}
