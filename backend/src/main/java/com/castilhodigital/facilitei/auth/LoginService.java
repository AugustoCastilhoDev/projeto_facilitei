package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.common.exception.CredenciaisInvalidasException;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.user.User;
import com.castilhodigital.facilitei.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse autenticar(String email, String senha) {
        User user;
        try {
            user = userService.buscarPorEmail(email);
        } catch (EntidadeNaoEncontradaException ex) {
            // mesma mensagem de erro para email inexistente ou senha errada,
            // para nao revelar a um atacante quais emails estao cadastrados
            throw new CredenciaisInvalidasException();
        }

        if (!passwordEncoder.matches(senha, user.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        String token = jwtService.gerarToken(user);
        return new LoginResponse(token, user.getTenant().getId(), user.getTenant().getSlug(), user.getEmail());
    }

}
