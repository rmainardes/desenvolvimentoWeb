package br.com.agenciaviagens.destinos.model;

/**
 * Perfis de acesso da API.
 *
 * O prefixo ROLE_ exigido pelo Spring Security nao e armazenado no banco:
 * ele e adicionado na montagem do UserDetails, mantendo o enum limpo.
 */
public enum Perfil {

    /** Consulta o catalogo e registra avaliacoes. */
    USER,

    /** Alem do que o USER faz, cadastra, altera e exclui destinos. */
    ADMIN;

    public String comoAuthority() {
        return "ROLE_" + name();
    }
}
