package br.com.agenciaviagens.destinos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Objeto de valor com a localizacao de um destino.
 *
 * Era um record na versao em memoria. Virou uma classe @Embeddable porque o
 * JPA precisa de um construtor sem argumentos para materializar componentes
 * embutidos. Os acessores mantem o estilo de record (cidade(), estado(),
 * pais()) de proposito: assim nenhum ponto que ja consumia esta classe
 * precisou mudar.
 *
 * Continua imutavel: os campos so sao escritos no construtor.
 */
@Embeddable
public class Localizacao {

    @Column(name = "cidade", nullable = false, length = 120)
    private String cidade;

    @Column(name = "estado", length = 120)
    private String estado;

    @Column(name = "pais", nullable = false, length = 120)
    private String pais;

    /** Exigido pelo JPA. Nao usar no codigo de dominio. */
    protected Localizacao() {
    }

    public Localizacao(String cidade, String estado, String pais) {
        this.cidade = normalizar(cidade);
        this.estado = normalizar(estado);
        this.pais = normalizar(pais);
    }

    private static String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    public String cidade() {
        return cidade;
    }

    public String estado() {
        return estado;
    }

    public String pais() {
        return pais;
    }

    /** Texto usado nas pesquisas por localizacao. */
    public String textoPesquisavel() {
        return String.join(" ", cidade == null ? "" : cidade,
                estado == null ? "" : estado,
                pais == null ? "" : pais);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Localizacao localizacao)) {
            return false;
        }
        return Objects.equals(cidade, localizacao.cidade)
                && Objects.equals(estado, localizacao.estado)
                && Objects.equals(pais, localizacao.pais);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cidade, estado, pais);
    }

    @Override
    public String toString() {
        return textoPesquisavel();
    }
}
