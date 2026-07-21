package com.castilhodigital.facilitei.scheduling;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint publico (sem autenticacao) para a pagina de agendamento do cliente final. */
@RestController
@RequestMapping("/api/public/tenants/{slug}/slots")
@RequiredArgsConstructor
public class PublicSlotController {

    private final SlotService slotService;

    @GetMapping
    public List<SlotResponse> listarDisponiveis(@PathVariable String slug,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return slotService.listarDisponiveisPorSlug(slug, data).stream()
                .map(SlotResponse::from)
                .toList();
    }

}
