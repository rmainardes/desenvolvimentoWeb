package br.com.agenciaviagens.destinos.repository.jpa;

import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.repository.DestinoRepository;
import br.com.agenciaviagens.destinos.util.Textos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador entre a porta DestinoRepository e o Spring Data JPA.
 *
 * Responsabilidade unica: traduzir a linguagem do dominio (nome, localizacao)
 * para a linguagem da consulta (padrao LIKE sobre texto normalizado). Nenhuma
 * regra de negocio mora aqui.
 */
@Repository
public class DestinoRepositoryJpa implements DestinoRepository {

    /** Caractere de escape do LIKE, declarado tambem na consulta. */
    private static final String ESCAPE = "!";

    /** Padrao que casa com qualquer valor, usado quando o filtro nao veio. */
    private static final String QUALQUER = "%";

    private final DestinoJpaRepository jpa;

    public DestinoRepositoryJpa(DestinoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Destino salvar(Destino destino) {
        return jpa.save(destino);
    }

    @Override
    public Optional<Destino> buscarPorId(UUID id) {
        return id == null ? Optional.empty() : jpa.findById(id);
    }

    @Override
    public Optional<Destino> buscarParaAtualizacao(UUID id) {
        return id == null ? Optional.empty() : jpa.buscarParaAtualizacao(id);
    }

    @Override
    public List<Destino> listar(String nome, String localizacao) {
        return jpa.pesquisar(paraPadraoLike(nome), paraPadraoLike(localizacao));
    }

    @Override
    public boolean existePorNomeELocalizacao(String nome, String cidade, String pais, UUID idIgnorado) {
        return jpa.findByNomeNormalizadoAndCidadeNormalizadaAndPaisNormalizado(
                        Textos.normalizar(nome), Textos.normalizar(cidade), Textos.normalizar(pais))
                .filter(existente -> idIgnorado == null || !existente.getId().equals(idIgnorado))
                .isPresent();
    }

    @Override
    public boolean remover(UUID id) {
        if (id == null || !jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    @Override
    public boolean vazio() {
        return jpa.count() == 0;
    }

    /**
     * Converte um filtro opcional em um padrao LIKE, nunca nulo.
     *
     * Filtro em branco vira "%", que casa com tudo. Nao usamos null aqui de
     * proposito: um parametro nulo chega ao PostgreSQL sem tipo declarado, ele
     * infere bytea, e a comparacao falha com "operator does not exist:
     * character varying ~~ bytea". Manter o parametro sempre textual evita a
     * classe inteira de problema.
     *
     * Os coringas do proprio LIKE (% e _) digitados pelo usuario sao
     * escapados, para que pesquisar por "50%" procure o texto "50%" e nao
     * qualquer coisa comecando com 50. O caractere de escape precisa ser
     * escapado primeiro, senao escaparia os escapes inseridos depois.
     */
    private String paraPadraoLike(String valor) {
        if (Textos.vazio(valor)) {
            return QUALQUER;
        }
        String termo = Textos.normalizar(valor)
                .replace(ESCAPE, ESCAPE + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
        return QUALQUER + termo + QUALQUER;
    }
}
