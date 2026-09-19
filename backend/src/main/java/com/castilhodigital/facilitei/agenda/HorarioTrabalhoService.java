package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Expediente semanal de cada profissional, editado como um todo (a tela salva a semana inteira de uma vez). */
@Service
@RequiredArgsConstructor
public class HorarioTrabalhoService {

    /** Um intervalo do expediente (hora de parede no fuso do negocio). */
    public record Intervalo(DayOfWeek diaSemana, LocalTime inicio, LocalTime fim) {
    }

    private final ProfissionalService profissionalService;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;

    @Transactional(readOnly = true)
    public List<Intervalo> listar(Long tenantId, Long profissionalId) {
        Profissional profissional = profissionalService.buscarPorIdETenant(tenantId, profissionalId);
        return horarioTrabalhoRepository.findByProfissionalIdOrderByDiaSemanaAscInicioAsc(profissional.getId()).stream()
                .sorted(Comparator.comparing(HorarioTrabalho::getDiaSemana).thenComparing(HorarioTrabalho::getInicio))
                .map(h -> new Intervalo(h.getDiaSemana(), h.getInicio(), h.getFim()))
                .toList();
    }

    /**
     * Substitui o expediente inteiro do profissional. Valida antes de tocar no banco para devolver um erro
     * claro ("terca: 09:00-12:00 se sobrepoe a 11:00-14:00"); a constraint do banco continua de guarda.
     * Lista vazia e valida: o profissional fica sem expediente e nao aparece para agendamento.
     */
    @Transactional
    public List<Intervalo> substituir(Long tenantId, Long profissionalId, List<Intervalo> novos) {
        Profissional profissional = profissionalService.buscarPorIdETenant(tenantId, profissionalId);
        validar(novos);

        // exclusao em massa ANTES de inserir: o Hibernate executa inserts antes de deletes no flush, e os
        // intervalos novos poderiam colidir com os antigos na constraint de sobreposicao
        horarioTrabalhoRepository.excluirTodosDoProfissional(profissional.getId());
        for (Intervalo intervalo : novos) {
            HorarioTrabalho horario = new HorarioTrabalho();
            horario.setProfissional(profissional);
            horario.setDiaSemana(intervalo.diaSemana());
            horario.setInicio(intervalo.inicio());
            horario.setFim(intervalo.fim());
            horarioTrabalhoRepository.save(horario);
        }
        horarioTrabalhoRepository.flush();
        return novos.stream()
                .sorted(Comparator.comparing(Intervalo::diaSemana).thenComparing(Intervalo::inicio))
                .toList();
    }

    private void validar(List<Intervalo> intervalos) {
        for (Intervalo intervalo : intervalos) {
            if (!intervalo.fim().isAfter(intervalo.inicio())) {
                throw new RegraDeNegocioException(
                        "O fim do expediente precisa ser depois do início (" + intervalo.inicio() + " - " + intervalo.fim() + ").");
            }
        }
        List<Intervalo> ordenados = intervalos.stream()
                .sorted(Comparator.comparing(Intervalo::diaSemana).thenComparing(Intervalo::inicio))
                .toList();
        for (int i = 1; i < ordenados.size(); i++) {
            Intervalo anterior = ordenados.get(i - 1);
            Intervalo atual = ordenados.get(i);
            if (anterior.diaSemana() == atual.diaSemana() && atual.inicio().isBefore(anterior.fim())) {
                throw new RegraDeNegocioException("Horários de trabalho se sobrepõem no mesmo dia (" + atual.diaSemana() + ": "
                        + anterior.inicio() + "-" + anterior.fim() + " e " + atual.inicio() + "-" + atual.fim() + ").");
            }
        }
    }

}
