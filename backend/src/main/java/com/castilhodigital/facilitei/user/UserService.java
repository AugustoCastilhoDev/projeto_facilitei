package com.castilhodigital.facilitei.user;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registrarAdmin(Tenant tenant, String email, String senha) {
        if (userRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("Ja existe um usuario cadastrado com o email '" + email + "'.");
        }

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(email);
        user.setSenhaHash(passwordEncoder.encode(senha));
        user.setRole(Role.ADMIN);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User buscarPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuario nao encontrado para o email '" + email + "'."));
    }

}
