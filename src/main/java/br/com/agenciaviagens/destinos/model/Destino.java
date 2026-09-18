package br.com.agenciaviagens.destinos.model;

import br.com.agenciaviagens.destinos.util.Textos;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Entidade de dominio que representa um destino turistico.
 *
 * O resumo de avaliacoes (nota media e total) e mantido aqui apenas como dado
 * derivado: quem recalcula e a camada de servico, a partir das avaliacoes
 * registradas.
 *
 * Sobre a persistencia:
 *
 * - O identificador e atribuido no construtor, e nao gerado pelo banco. Isso
 *   mantem o objeto valido desde a criacao e permite testar o dominio sem
 *   persistencia. Como consequencia, o id nunca e nulo e o Spring Data nao
 *   consegue deduzir sozinho se a entidade e nova; por isso a classe
 *   implementa Persistable e controla essa informacao com a flag "novo",
 *   evitando um SELECT desnecessario antes de cada INSERT.
 *
 * - Os campos *Normalizado guardam a versao sem acentos e em minusculas dos
 *   dados usados em pesquisa e na regra de unicidade. Eles sao recalculados
 *   sempre que o nome ou a localizacao mudam, e nunca sao expostos pela API.
 */
@Entity
@Table(name = "destino")
public class Destino implements Persistable<UUID> {

    public static final int ESCALA_NOTA = 2;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 120)
    private String nomeNormalizado;

    @Column(name = "descricao", nullable = false, length = 2000)
    private String descricao;

    @Embedded
    private Localizacao localizacao;

    @Column(name = "cidade_normalizada", nullable = false, length = 120)
    private String cidadeNormalizada;

    @Column(name = "pais_normalizado", nullable = false, length = 120)
    private String paisNormalizado;

    @Column(name = "localizacao_normalizada", nullable = false, length = 400)
    private String localizacaoNormalizada;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "destino_atividade",
            joinColumns = @JoinColumn(
                    name = "destino_id",
                    foreignKey = @ForeignKey(name = "fk_destino_atividade_destino")))
    @OrderColumn(name = "ordem")
    @Column(name = "atividade", nullable = false, length = 80)
    @BatchSize(size = 25)
    private List<String> atividades = new ArrayList<>();

    @Column(name = "hoteis_disponiveis", nullable = false)
    private int hoteisDisponiveis;

    @Column(name = "preco_pacote", nullable = false, precision = 11, scale = 2)
    private BigDecimal precoPacote;

    @Column(name = "nota_media", nullable = false, precision = 3, scale = 2)
    private BigDecimal notaMedia;

    @Column(name = "total_avaliacoes", nullable = false)
    private int totalAvaliacoes;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Transient
    private boolean novo = true;

    /** Exigido pelo JPA. Nao usar no codigo de dominio. */
    protected Destino() {
    }

    public Destino(String nome,
                   String descricao,
                   Localizacao localizacao,
                   List<String> atividades,
                   int hoteisDisponiveis,
                   BigDecimal precoPacote) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.descricao = descricao;
        this.localizacao = localizacao;
        substituirAtividades(atividades);
        this.hoteisDisponiveis = hoteisDisponiveis;
        this.precoPacote = precoPacote;
        this.notaMedia = BigDecimal.ZERO.setScale(ESCALA_NOTA, RoundingMode.HALF_UP);
        this.totalAvaliacoes = 0;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
        recalcularCamposDePesquisa();
    }

    /** Substitui os dados cadastrais do destino (usado por PUT e PATCH). */
    public void atualizarDados(String nome,
                               String descricao,
                               Localizacao localizacao,
                               List<String> atividades,
                               int hoteisDisponiveis,
                               BigDecimal precoPacote) {
        this.nome = nome;
        this.descricao = descricao;
        this.localizacao = localizacao;
        substituirAtividades(atividades);
        this.hoteisDisponiveis = hoteisDisponiveis;
        this.precoPacote = precoPacote;
        this.atualizadoEm = Instant.now();
        recalcularCamposDePesquisa();
    }

    /** Atualiza o resumo de avaliacoes calculado pela camada de servico. */
    public void atualizarResumoAvaliacoes(BigDecimal notaMedia, int totalAvaliacoes) {
        this.notaMedia = notaMedia.setScale(ESCALA_NOTA, RoundingMode.HALF_UP);
        this.totalAvaliacoes = totalAvaliacoes;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Troca o conteudo da colecao sem trocar a instancia.
     *
     * O JPA rastreia a colecao pela referencia: atribuir uma lista nova faria
     * o Hibernate perder o rastreio da original. Por isso limpamos e
     * repovoamos a mesma instancia. A copia defensiva antes do clear() e
     * necessaria porque o chamador pode estar passando justamente a lista
     * devolvida por getAtividades(), que e uma visao da colecao interna --
     * limpar antes de copiar apagaria os proprios valores recebidos.
     */
    private void substituirAtividades(List<String> valores) {
        List<String> copia = valores == null ? List.of() : List.copyOf(valores);
        this.atividades.clear();
        this.atividades.addAll(copia);
    }

    private void recalcularCamposDePesquisa() {
        this.nomeNormalizado = Textos.normalizar(nome);
        this.cidadeNormalizada = Textos.normalizar(localizacao == null ? null : localizacao.cidade());
        this.paisNormalizado = Textos.normalizar(localizacao == null ? null : localizacao.pais());
        this.localizacaoNormalizada =
                Textos.normalizar(localizacao == null ? null : localizacao.textoPesquisavel());
    }

    @PostPersist
    @PostLoad
    void marcarComoPersistido() {
        this.novo = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeNormalizado() {
        return nomeNormalizado;
    }

    public String getDescricao() {
        return descricao;
    }

    public Localizacao getLocalizacao() {
        return localizacao;
    }

    public String getCidadeNormalizada() {
        return cidadeNormalizada;
    }

    public String getPaisNormalizado() {
        return paisNormalizado;
    }

    public String getLocalizacaoNormalizada() {
        return localizacaoNormalizada;
    }

    public List<String> getAtividades() {
        return Collections.unmodifiableList(atividades);
    }

    public int getHoteisDisponiveis() {
        return hoteisDisponiveis;
    }

    public BigDecimal getPrecoPacote() {
        return precoPacote;
    }

    public BigDecimal getNotaMedia() {
        return notaMedia;
    }

    public int getTotalAvaliacoes() {
        return totalAvaliacoes;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
