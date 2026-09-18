package br.com.agenciaviagens.destinos.repository.jpa;

import br.com.agenciaviagens.destinos.model.Destino;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data sobre a entidade Destino.
 *
 * Fica separado da porta DestinoRepository de proposito: aqui vivem apenas
 * consultas, sem regra de negocio. Quem traduz uma para a outra e o adaptador
 * DestinoRepositoryJpa.
 */
public interface DestinoJpaRepository extends JpaRepository<Destino, UUID> {

    /**
     * Pesquisa por nome e/ou localizacao, ambos opcionais.
     *
     * A comparacao usa as colunas normalizadas (sem acento, em minusculas),
     * entao "sao paulo" encontra "Sao Paulo" sem depender da extensao unaccent
     * do PostgreSQL.
     *
     * Os parametros chegam do adaptador ja normalizados e ja no formato de
     * padrao LIKE, e nunca nulos: filtro ausente vira "%". Isso e deliberado.
     * A forma intuitiva, "(:nome is null or ... like ...)", gera um parametro
     * nulo sem tipo declarado, e o PostgreSQL infere bytea para ele --
     * resultando em "operator does not exist: character varying ~~ bytea" em
     * toda consulta sem filtro. Manter o parametro sempre textual resolve na
     * origem, sem precisar de cast explicito, e ainda simplifica o SQL.
     *
     * O escape '!' neutraliza % e _ digitados pelo usuario, que de outro modo
     * seriam interpretados como coringa.
     */
    @Query("""
            select d
              from Destino d
             where d.nomeNormalizado like :nome escape '!'
               and d.localizacaoNormalizada like :localizacao escape '!'
             order by d.nomeNormalizado
            """)
    List<Destino> pesquisar(@Param("nome") String nome, @Param("localizacao") String localizacao);

    Optional<Destino> findByNomeNormalizadoAndCidadeNormalizadaAndPaisNormalizado(
            String nomeNormalizado, String cidadeNormalizada, String paisNormalizado);

    /**
     * Le a linha do destino com lock de escrita (SELECT ... FOR UPDATE).
     *
     * Usado ao registrar uma avaliacao: garante que o recalculo da media
     * enxergue um estado consistente mesmo com requisicoes concorrentes para o
     * mesmo destino.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Destino d where d.id = :id")
    Optional<Destino> buscarParaAtualizacao(@Param("id") UUID id);
}
