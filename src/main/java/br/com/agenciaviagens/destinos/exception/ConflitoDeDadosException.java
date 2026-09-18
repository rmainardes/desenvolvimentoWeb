package br.com.agenciaviagens.destinos.exception;

/**
 * Lancada quando a operacao violaria uma regra de unicidade do negocio.
 * Mapeada para HTTP 409.
 */
public class ConflitoDeDadosException extends RuntimeException {

    public ConflitoDeDadosException(String mensagem) {
        super(mensagem);
    }
}
