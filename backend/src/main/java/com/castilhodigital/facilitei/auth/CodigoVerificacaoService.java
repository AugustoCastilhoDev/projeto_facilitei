package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.common.exception.CodigoVerificacaoInvalidoException;
import com.castilhodigital.facilitei.notification.OtpNotificationSender;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera e valida codigos de verificacao (OTP) de 6 digitos para o login do
 * portal do cliente. O codigo nunca e gravado em texto puro (reusa o mesmo
 * PasswordEncoder/BCrypt ja usado para senha de admin).
 */
@Service
@RequiredArgsConstructor
public class CodigoVerificacaoService {

    private static final int TAMANHO_CODIGO = 6;
    private static final Duration VALIDADE = Duration.ofMinutes(5);
    private static final int MAX_TENTATIVAS = 5;

    private final CodigoVerificacaoRepository codigoVerificacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpNotificationSender otpNotificationSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void gerarEEnviar(String telefone) {
        String codigo = gerarCodigoNumerico();

        CodigoVerificacao entidade = new CodigoVerificacao();
        entidade.setTelefone(telefone);
        entidade.setCodigoHash(passwordEncoder.encode(codigo));
        entidade.setExpiraEm(Instant.now().plus(VALIDADE));
        entidade.setCreatedAt(Instant.now());
        codigoVerificacaoRepository.save(entidade);

        // fora do best-effort de NotificationService de proposito: se o envio
        // falhar, a excecao propaga e a transacao inteira (incluindo a linha
        // recem-criada acima) e desfeita - nao faz sentido guardar um codigo
        // que o cliente nunca recebeu.
        otpNotificationSender.enviarCodigo(telefone, codigo);
    }

    @Transactional
    public void verificar(String telefone, String codigoDigitado) {
        CodigoVerificacao codigo = codigoVerificacaoRepository
                .findFirstByTelefoneAndUsadoEmIsNullOrderByCreatedAtDesc(telefone)
                .orElseThrow(CodigoVerificacaoInvalidoException::new);

        if (codigo.getExpiraEm().isBefore(Instant.now()) || codigo.getTentativas() >= MAX_TENTATIVAS) {
            throw new CodigoVerificacaoInvalidoException();
        }

        if (!passwordEncoder.matches(codigoDigitado, codigo.getCodigoHash())) {
            codigo.setTentativas(codigo.getTentativas() + 1);
            throw new CodigoVerificacaoInvalidoException();
        }

        codigo.setUsadoEm(Instant.now());
    }

    private String gerarCodigoNumerico() {
        int valor = secureRandom.nextInt((int) Math.pow(10, TAMANHO_CODIGO));
        return String.format("%0" + TAMANHO_CODIGO + "d", valor);
    }

}
