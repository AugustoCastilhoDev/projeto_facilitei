package com.castilhodigital.facilitei.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.common.exception.CodigoVerificacaoInvalidoException;
import com.castilhodigital.facilitei.notification.OtpNotificationSender;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CodigoVerificacaoServiceTest {

    @Mock
    private CodigoVerificacaoRepository codigoVerificacaoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpNotificationSender otpNotificationSender;

    private CodigoVerificacaoService codigoVerificacaoService;

    @BeforeEach
    void setUp() {
        codigoVerificacaoService = new CodigoVerificacaoService(codigoVerificacaoRepository, passwordEncoder, otpNotificationSender);
    }

    @Test
    void gerarEEnviarPersisteCodigoHasheadoEEnviaOMesmoCodigoEmTextoPuro() {
        when(passwordEncoder.encode(any())).thenReturn("hash-fake");

        codigoVerificacaoService.gerarEEnviar("5563999999999");

        ArgumentCaptor<String> codigoParaHash = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(codigoParaHash.capture());

        ArgumentCaptor<String> codigoEnviado = ArgumentCaptor.forClass(String.class);
        verify(otpNotificationSender).enviarCodigo(eq("5563999999999"), codigoEnviado.capture());

        assertThat(codigoParaHash.getValue()).isEqualTo(codigoEnviado.getValue());
        assertThat(codigoParaHash.getValue()).hasSize(6).containsOnlyDigits();

        ArgumentCaptor<CodigoVerificacao> entidadeCaptor = ArgumentCaptor.forClass(CodigoVerificacao.class);
        verify(codigoVerificacaoRepository).save(entidadeCaptor.capture());
        assertThat(entidadeCaptor.getValue().getCodigoHash()).isEqualTo("hash-fake");
        assertThat(entidadeCaptor.getValue().getExpiraEm()).isAfter(Instant.now());
    }

    private CodigoVerificacao codigoValido() {
        CodigoVerificacao codigo = new CodigoVerificacao();
        codigo.setTelefone("5563999999999");
        codigo.setCodigoHash("hash-fake");
        codigo.setExpiraEm(Instant.now().plusSeconds(300));
        codigo.setTentativas(0);
        return codigo;
    }

    @Test
    void verificarComCodigoCorretoMarcaComoUsado() {
        CodigoVerificacao codigo = codigoValido();
        when(codigoVerificacaoRepository.findFirstByTelefoneAndUsadoEmIsNullOrderByCreatedAtDesc("5563999999999"))
                .thenReturn(Optional.of(codigo));
        when(passwordEncoder.matches("123456", "hash-fake")).thenReturn(true);

        codigoVerificacaoService.verificar("5563999999999", "123456");

        assertThat(codigo.getUsadoEm()).isNotNull();
    }

    @Test
    void verificarComCodigoErradoIncrementaTentativasELancaExcecao() {
        CodigoVerificacao codigo = codigoValido();
        when(codigoVerificacaoRepository.findFirstByTelefoneAndUsadoEmIsNullOrderByCreatedAtDesc("5563999999999"))
                .thenReturn(Optional.of(codigo));
        when(passwordEncoder.matches("000000", "hash-fake")).thenReturn(false);

        assertThatThrownBy(() -> codigoVerificacaoService.verificar("5563999999999", "000000"))
                .isInstanceOf(CodigoVerificacaoInvalidoException.class);

        assertThat(codigo.getTentativas()).isEqualTo(1);
        assertThat(codigo.getUsadoEm()).isNull();
    }

    @Test
    void verificarCodigoExpiradoLancaExcecaoSemChamarPasswordEncoder() {
        CodigoVerificacao codigo = codigoValido();
        codigo.setExpiraEm(Instant.now().minusSeconds(1));
        when(codigoVerificacaoRepository.findFirstByTelefoneAndUsadoEmIsNullOrderByCreatedAtDesc("5563999999999"))
                .thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> codigoVerificacaoService.verificar("5563999999999", "123456"))
                .isInstanceOf(CodigoVerificacaoInvalidoException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void verificarSemCodigoSolicitadoLancaExcecao() {
        when(codigoVerificacaoRepository.findFirstByTelefoneAndUsadoEmIsNullOrderByCreatedAtDesc("5563999999999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> codigoVerificacaoService.verificar("5563999999999", "123456"))
                .isInstanceOf(CodigoVerificacaoInvalidoException.class);
    }

    @Test
    void verificarComTentativasNoLimiteLancaExcecaoSemChamarPasswordEncoder() {
        CodigoVerificacao codigo = codigoValido();
        codigo.setTentativas(5);
        when(codigoVerificacaoRepository.findFirstByTelefoneAndUsadoEmIsNullOrderByCreatedAtDesc("5563999999999"))
                .thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> codigoVerificacaoService.verificar("5563999999999", "123456"))
                .isInstanceOf(CodigoVerificacaoInvalidoException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

}
