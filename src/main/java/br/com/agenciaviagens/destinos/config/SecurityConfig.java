package br.com.agenciaviagens.destinos.config;

import br.com.agenciaviagens.destinos.security.TratadorDeErrosDeSeguranca;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao de seguranca da API.
 *
 * ---------------------------------------------------------------------------
 * Regras de acesso
 * ---------------------------------------------------------------------------
 * A agencia expoe o catalogo para parceiros e aplicativos, mas o conteudo do
 * catalogo e o cadastro da propria agencia. Dai os tres niveis:
 *
 *   publico      GET /api/v1/destinos/**        consultar e pesquisar destinos
 *                                               (a vitrine precisa ser aberta)
 *   autenticado  POST .../{id}/avaliacoes       avaliar exige identidade: nota
 *                                               anonima e vetor de manipulacao
 *   ADMIN        POST, PUT, PATCH, DELETE       manter o catalogo e operacao
 *                de /api/v1/destinos            interna da agencia
 *
 * ---------------------------------------------------------------------------
 * Por que Basic e nao formulario de login
 * ---------------------------------------------------------------------------
 * A API e stateless e consumida por outros sistemas, nao por um navegador.
 * Sessao e tela de login nao teriam consumidor. Basic sobre HTTPS resolve o
 * caso e mantem o contrato simples de integrar. Sem HTTPS a credencial
 * trafega em base64, que nao e criptografia -- por isso o README traz TLS como
 * pre-requisito de qualquer ambiente que nao seja a maquina do desenvolvedor.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Argon2id para as senhas.
     *
     * Parametros explicitos em vez de defaultsForSpringSecurity_v5_8(): aquele
     * preset usa 16 MiB de memoria, e o minimo recomendado hoje pela OWASP
     * (Password Storage Cheat Sheet) e 19 MiB com t=2 e p=1. Argumentos, na
     * ordem: tamanho do salt em bytes, tamanho do hash em bytes, paralelismo,
     * memoria em KiB, iteracoes.
     *
     * O encoder e embrulhado em um DelegatingPasswordEncoder, entao o hash
     * gravado carrega o prefixo do algoritmo -- {argon2}$argon2id$v=19$m=19456,...
     * Duas consequencias praticas:
     *
     *   - trocar de algoritmo mais tarde nao invalida as senhas existentes: os
     *     hashes antigos continuam sendo verificados pelo encoder
     *     correspondente e so os novos usam o algoritmo novo;
     *   - bcrypt segue registrado apenas para leitura de hashes legados. O
     *     algoritmo usado para codificar e sempre o primeiro argumento,
     *     argon2.
     *
     * Custo a ter em mente: sao 19 MiB alocados por hash em andamento. Com
     * muitos logins simultaneos isso dimensiona junto com o pool de threads.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);

        Map<String, PasswordEncoder> algoritmos = Map.of(
                "argon2", argon2,
                "bcrypt", new BCryptPasswordEncoder());

        return new DelegatingPasswordEncoder("argon2", algoritmos);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           TratadorDeErrosDeSeguranca tratadorDeErros) throws Exception {
        http
                // CSRF protege navegadores que enviam credencial automaticamente
                // (cookie de sessao). Com Basic stateless nao ha o que sequestrar:
                // o cliente precisa mandar o header a cada requisicao.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        // Consulta publica do catalogo.
                        .requestMatchers(HttpMethod.GET, "/api/v1/destinos", "/api/v1/destinos/**")
                                .permitAll()
                        // Avaliar exige estar autenticado, em qualquer perfil.
                        // Precisa vir antes da regra generica de escrita abaixo.
                        .requestMatchers(HttpMethod.POST, "/api/v1/destinos/*/avaliacoes")
                                .hasAnyRole("USER", "ADMIN")
                        // Todo o resto do recurso destinos e manutencao de catalogo.
                        .requestMatchers("/api/v1/destinos", "/api/v1/destinos/**")
                                .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(tratadorDeErros))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(tratadorDeErros)
                        .accessDeniedHandler(tratadorDeErros));

        return http.build();
    }
}
