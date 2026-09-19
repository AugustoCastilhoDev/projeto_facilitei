package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.time.ZoneId;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Parametros da agenda de cada negocio: fuso, grade de horarios, antecedencia minima e horizonte. */
@Service
@RequiredArgsConstructor
public class AgendaConfigService {

    public record AgendaConfig(String fusoHorario, int intervaloAgendaMin, int antecedenciaMinimaMin, int horizonteDias) {

        static AgendaConfig de(Tenant tenant) {
            return new AgendaConfig(tenant.getFusoHorario(), tenant.getIntervaloAgendaMin(),
                    tenant.getAntecedenciaMinimaMin(), tenant.getHorizonteDias());
        }
    }

    /** Os mesmos valores que a constraint chk_tenants_intervalo_agenda do banco aceita. */
    static final Set<Integer> INTERVALOS_PERMITIDOS = Set.of(5, 10, 15, 20, 30, 60);
    static final int ANTECEDENCIA_MAXIMA_MIN = 7 * 24 * 60;
    static final int HORIZONTE_MAXIMO_DIAS = 365;

    private final TenantService tenantService;

    @Transactional(readOnly = true)
    public AgendaConfig buscar(Long tenantId) {
        return AgendaConfig.de(tenantService.buscarPorId(tenantId));
    }

    @Transactional
    public AgendaConfig atualizar(Long tenantId, AgendaConfig nova) {
        validar(nova);
        Tenant tenant = tenantService.buscarPorId(tenantId);
        tenant.setFusoHorario(nova.fusoHorario());
        tenant.setIntervaloAgendaMin(nova.intervaloAgendaMin());
        tenant.setAntecedenciaMinimaMin(nova.antecedenciaMinimaMin());
        tenant.setHorizonteDias(nova.horizonteDias());
        return AgendaConfig.de(tenant);
    }

    private void validar(AgendaConfig config) {
        if (config.fusoHorario() == null || !ZoneId.getAvailableZoneIds().contains(config.fusoHorario())) {
            throw new RegraDeNegocioException("Fuso horário inválido (use um identificador como America/Sao_Paulo).");
        }
        if (!INTERVALOS_PERMITIDOS.contains(config.intervaloAgendaMin())) {
            throw new RegraDeNegocioException("O intervalo da agenda deve ser 5, 10, 15, 20, 30 ou 60 minutos.");
        }
        if (config.antecedenciaMinimaMin() < 0 || config.antecedenciaMinimaMin() > ANTECEDENCIA_MAXIMA_MIN) {
            throw new RegraDeNegocioException("A antecedência mínima deve ficar entre 0 e " + ANTECEDENCIA_MAXIMA_MIN + " minutos.");
        }
        if (config.horizonteDias() < 1 || config.horizonteDias() > HORIZONTE_MAXIMO_DIAS) {
            throw new RegraDeNegocioException("O horizonte da agenda deve ficar entre 1 e " + HORIZONTE_MAXIMO_DIAS + " dias.");
        }
    }

}
