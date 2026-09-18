package br.com.agenciaviagens.destinos.exception;

/**
 * Lancada quando o recurso solicitado nao existe. Mapeada para HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
