package com.castilhodigital.facilitei.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void excecaoNaoMapeadaViraErroInternoGenericoSemVazarDetalhe() {
        ProblemDetail problemDetail = handler.handleInesperada(new RuntimeException("detalhe interno sensivel"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).doesNotContain("detalhe interno sensivel");
    }

}
