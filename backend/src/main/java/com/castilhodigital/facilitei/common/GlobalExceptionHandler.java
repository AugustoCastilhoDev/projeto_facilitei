package com.castilhodigital.facilitei.common;

import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import com.castilhodigital.facilitei.common.exception.CredenciaisInvalidasException;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import io.sentry.Sentry;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz as excecoes de dominio para respostas HTTP no formato RFC 7807
 * (ProblemDetail, suportado nativamente pelo Spring) em vez de um formato
 * de erro proprio.
 *
 * Estende ResponseEntityExceptionHandler para herdar o tratamento default
 * do Spring MVC para excecoes de infraestrutura (ex.:
 * HttpMessageNotReadableException para JSON malformado,
 * MissingServletRequestParameterException para parametro obrigatorio
 * ausente, NoHandlerFoundException etc.) - todas ja continuam virando o
 * 4xx correto porque um @ExceptionHandler para o tipo exato sempre vence
 * sobre o catch-all generico de RuntimeException abaixo, que so deveria
 * pegar bugs de verdade, nao erro de input do cliente.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntidadeNaoEncontradaException.class)
    public ProblemDetail handleEntidadeNaoEncontrada(EntidadeNaoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail handleRegraDeNegocio(RegraDeNegocioException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ProblemDetail handleAcessoNegado(AcessoNegadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(LimiteDeRequisicoesExcedidoException.class)
    public ProblemDetail handleLimiteDeRequisicoes(LimiteDeRequisicoesExcedidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ProblemDetail handlePaymentGateway(PaymentGatewayException ex) {
        log.error("Falha na integracao com o gateway de pagamento", ex);
        Sentry.captureException(ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "Nao foi possivel gerar a cobranca no momento. Tente novamente em instantes.");
    }

    /**
     * Sobrescreve o metodo protegido herdado (nao um @ExceptionHandler
     * proprio para o mesmo tipo): ResponseEntityExceptionHandler ja mapeia
     * MethodArgumentNotValidException no seu handleException() abrangente,
     * entao declarar um @ExceptionHandler direto para o mesmo tipo aqui gera
     * "Ambiguous @ExceptionHandler" na inicializacao (ambos com a mesma
     * especificidade de tipo) - erro real, pego ao rodar a suite de testes.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados invalidos.");
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erros.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problemDetail.setProperty("erros", erros);
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Ultimo recurso: qualquer RuntimeException nao mapeada acima (nem pelos
     * handlers especificos deste arquivo, nem pelos herdados de
     * ResponseEntityExceptionHandler para excecoes conhecidas do Spring MVC)
     * e um bug real, nao um erro de input esperado - por isso e a unica
     * logada em ERROR com stacktrace completo e enviada ao Sentry.
     */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleInesperada(RuntimeException ex) {
        log.error("Erro inesperado", ex);
        Sentry.captureException(ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.");
    }

}
