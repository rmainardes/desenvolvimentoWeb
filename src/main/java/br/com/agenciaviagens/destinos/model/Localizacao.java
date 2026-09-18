package br.com.agenciaviagens.destinos.model;

/**
 * Objeto de valor imutavel com a localizacao de um destino.
 */
public record Localizacao(String cidade, String estado, String pais) {

    public Localizacao {
        cidade = normalizar(cidade);
        estado = normalizar(estado);
        pais = normalizar(pais);
    }

    private static String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    /** Texto usado nas pesquisas por localizacao. */
    public String textoPesquisavel() {
        return String.join(" ", cidade == null ? "" : cidade,
                estado == null ? "" : estado,
                pais == null ? "" : pais);
    }
}
