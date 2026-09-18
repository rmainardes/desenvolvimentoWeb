package br.com.agenciaviagens.destinos.exception;

import br.com.agenciaviagens.destinos.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz excecoes em respostas HTTP padronizadas.
 * Nenhuma mensagem interna (stack trace, SQL, nome de classe) e devolvida ao cliente.
 */
@RestControllerAdvice
public class TratadorGlobalDeExcecoes {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeExcecoes.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException excecao,
                                                            HttpServletRequest requisicao) {
        return construir(HttpStatus.NOT_FOUND, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(ConflitoDeDadosException.class)
    public ResponseEntity<ErroResponse> tratarConflito(ConflitoDeDadosException excecao,
                                                       HttpServletRequest requisicao) {
        return construir(HttpStatus.CONFLICT, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarRequisicaoInvalida(RequisicaoInvalidaException excecao,
                                                                 HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException excecao,
                                                        HttpServletRequest requisicao) {
        List<ErroResponse.CampoInvalido> campos = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoInvalido(erro.getField(), mensagemDe(erro)))
                .sorted(Comparator.comparing(ErroResponse.CampoInvalido::campo))
                .toList();

        ErroResponse corpo = ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Existem campos invalidos na requisicao",
                requisicao.getRequestURI(),
                campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException excecao,
                                                            HttpServletRequest requisicao) {
        log.warn("Corpo de requisicao invalido em {}", requisicao.getRequestURI());
        return construir(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao ausente ou em formato invalido", requisicao);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException excecao,
                                                                HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST,
                "Valor invalido para o parametro '" + excecao.getName() + "'", requisicao);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception excecao, HttpServletRequest requisicao) {
        log.error("Erro inesperado ao processar {}", requisicao.getRequestURI(), excecao);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno ao processar a requisicao", requisicao);
    }

    private static String mensagemDe(FieldError erro) {
        return erro.getDefaultMessage() == null ? "Valor invalido" : erro.getDefaultMessage();
    }

    private ResponseEntity<ErroResponse> construir(HttpStatus status, String mensagem,
                                                   HttpServletRequest requisicao) {
        ErroResponse corpo = ErroResponse.de(status.value(), status.getReasonPhrase(),
                mensagem, requisicao.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
