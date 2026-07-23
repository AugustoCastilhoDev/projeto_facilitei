package com.castilhodigital.facilitei.report;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatorio basico do admin (faturamento, taxa de nao comparecimento,
 * clientes recorrentes) - mesma protecao dos demais controllers admin.
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/relatorios")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportService reportService;
    private final TenantSecurityGuard tenantSecurityGuard;

    @GetMapping
    public RelatorioResponse gerar(@PathVariable Long tenantId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        tenantSecurityGuard.verificarAcessoAoTenant(tenantId);
        if (fim.isBefore(inicio)) {
            throw new RegraDeNegocioException("A data final nao pode ser anterior a data inicial.");
        }
        return reportService.gerarRelatorio(tenantId, inicio, fim);
    }

}
