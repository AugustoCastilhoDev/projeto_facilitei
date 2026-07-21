package com.castilhodigital.facilitei.common;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Falhas de autenticacao/autorizacao (401/403) acontecem no filtro do Spring
 * Security, antes do request chegar num @Controller - por isso nao passam
 * pelo GlobalExceptionHandler. Este helper escreve o mesmo formato
 * ProblemDetail (RFC 7807) manualmente, para a API responder de forma
 * consistente em qualquer tipo de erro.
 */
public final class ProblemDetailResponseWriter {

    private ProblemDetailResponseWriter() {
    }

    public static void escrever(HttpServletResponse response, ObjectMapper objectMapper,
                                 HttpStatus status, ProblemDetail problemDetail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }

}
