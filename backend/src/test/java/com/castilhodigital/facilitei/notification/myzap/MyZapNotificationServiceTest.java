package com.castilhodigital.facilitei.notification.myzap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.castilhodigital.facilitei.notification.NotificationGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MyZapNotificationServiceTest {

    @Mock
    private MyZapClient myZapClient;

    private MyZapNotificationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MyZapNotificationService(myZapClient);
    }

    @Test
    void enviarNormalizaTelefoneParaSoDigitosAntesDeChamarOClient() {
        service.enviar("+55 (11) 99999-8888", "Ola");

        verify(myZapClient).enviarTexto("5511999998888", "Ola");
    }

    @Test
    void falhaNoGatewayNaoPropagaParaNaoDerrubarATransacaoDaReserva() {
        doThrow(new NotificationGatewayException("falha simulada", null))
                .when(myZapClient)
                .enviarTexto("5511999998888", "Ola");

        assertThatCode(() -> service.enviar("+5511999998888", "Ola")).doesNotThrowAnyException();
    }

}
