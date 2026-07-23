package com.castilhodigital.facilitei.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void geraRequestIdQuandoAusenteELimpaMdcDepois() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdDuranteChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> requestIdDuranteChain.set(MDC.get(RequestCorrelationFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(requestIdDuranteChain.get()).isNotBlank();
        assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).isEqualTo(requestIdDuranteChain.get());
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void reaproveitaRequestIdRecebidoNoHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "meu-id-existente");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdDuranteChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> requestIdDuranteChain.set(MDC.get(RequestCorrelationFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(requestIdDuranteChain.get()).isEqualTo("meu-id-existente");
        assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).isEqualTo("meu-id-existente");
    }

}
