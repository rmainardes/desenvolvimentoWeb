package br.com.agenciaviagens.destinos.exception;

/**
 * Lancada quando a requisicao e sintaticamente valida, mas nao faz sentido
 * para o negocio (por exemplo, um PATCH sem nenhum campo). Mapeada para HTTP 400.
 */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
