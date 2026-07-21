package com.castilhodigital.facilitei.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean isolado do restante da configuracao de seguranca (SecurityFilterChain,
 * JWT) que sera adicionada na etapa 5. Definido aqui porque hash de senha e
 * uma preocupacao do dominio (UserService), nao da camada HTTP.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
