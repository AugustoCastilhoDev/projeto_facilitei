package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Folgas, ferias e ausencias de um profissional: periodos em que ele nao recebe agendamentos. */
@Service
@RequiredArgsConstructor
public class BloqueioService {

    public record BloqueioView(Long id, Long profissionalId, Instant inicio, Instant fim, String motivo) {

        static BloqueioView de(Bloqueio bloqueio) {
            return new BloqueioView(bloqueio.getId(), bloqueio.getProfissional().getId(),
                    bloqueio.getInicio(), bloqueio.getFim(), bloqueio.getMotivo());
        }
    }

    /**
     * @param agendamentosAfetados quantos agendamentos ja marcados caem dentro do periodo bloqueado. Bloquear NAO
     *                             cancela ninguem (isso e decisao do dono, caso a caso) - so avisa, para ele resolver.
     */
    public record BloqueioCriado(BloqueioView bloqueio, int agendamentosAfetados) {
    }

    private final ProfissionalService profissionalService;
    private final BloqueioRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public List<BloqueioView> listarEmAndamentoOuFuturos(Long tenantId, Long profissionalId, Instant agora) {
        Profissional profissional = profissionalService.buscarPorIdETenant(tenantId, profissionalId);
        return bloqueioRepository.findByProfissionalIdAndFimAfterOrderByInicio(profissional.getId(), agora).stream()
                .map(BloqueioView::de)
                .toList();
    }

    @Transactional
    public BloqueioCriado criar(Long tenantId, Long profissionalId, Instant inicio, Instant fim, String motivo) {
        Profissional profissional = profissionalService.buscarPorIdETenant(tenantId, profissionalId);
        if (!fim.isAfter(inicio)) {
            throw new RegraDeNegocioException("O fim do bloqueio precisa ser depois do início.");
        }

        Bloqueio bloqueio = new Bloqueio();
        bloqueio.setProfissional(profissional);
        bloqueio.setInicio(inicio);
        bloqueio.setFim(fim);
        bloqueio.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.trim());
        bloqueio = bloqueioRepository.save(bloqueio);

        int afetados = agendamentoRepository
                .findOcupados(List.of(profissional.getId()), AgendamentoStatus.QUE_OCUPAM_HORARIO, inicio, fim).size();
        return new BloqueioCriado(BloqueioView.de(bloqueio), afetados);
    }

    @Transactional
    public void excluir(Long tenantId, Long profissionalId, Long bloqueioId) {
        Bloqueio bloqueio = bloqueioRepository
                .findByIdAndProfissionalIdAndProfissionalTenantId(bloqueioId, profissionalId, tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Bloqueio não encontrado (id=" + bloqueioId + ")."));
        bloqueioRepository.delete(bloqueio);
    }

}
