package br.com.agenciaviagens.destinos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Entidade de dominio que representa um destino turistico.
 *
 * O resumo de avaliacoes (nota media e total) e mantido aqui apenas como
 * dado derivado: quem recalcula e a camada de servico, a partir das
 * avaliacoes registradas.
 */
public class Destino {

    public static final int ESCALA_NOTA = 2;

    private final UUID id;
    private String nome;
    private String descricao;
    private Localizacao localizacao;
    private List<String> atividades;
    private int hoteisDisponiveis;
    private BigDecimal precoPacote;
    private BigDecimal notaMedia;
    private int totalAvaliacoes;
    private final Instant criadoEm;
    private Instant atualizadoEm;

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
        this.atividades = copiar(atividades);
        this.hoteisDisponiveis = hoteisDisponiveis;
        this.precoPacote = precoPacote;
        this.notaMedia = BigDecimal.ZERO.setScale(ESCALA_NOTA, RoundingMode.HALF_UP);
        this.totalAvaliacoes = 0;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
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
        this.atividades = copiar(atividades);
        this.hoteisDisponiveis = hoteisDisponiveis;
        this.precoPacote = precoPacote;
        this.atualizadoEm = Instant.now();
    }

    /** Atualiza o resumo de avaliacoes calculado pela camada de servico. */
    public void atualizarResumoAvaliacoes(BigDecimal notaMedia, int totalAvaliacoes) {
        this.notaMedia = notaMedia.setScale(ESCALA_NOTA, RoundingMode.HALF_UP);
        this.totalAvaliacoes = totalAvaliacoes;
        this.atualizadoEm = Instant.now();
    }

    private static List<String> copiar(List<String> valores) {
        return valores == null ? new ArrayList<>() : new ArrayList<>(valores);
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Localizacao getLocalizacao() {
        return localizacao;
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
