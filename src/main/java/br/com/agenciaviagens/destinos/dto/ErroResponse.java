package br.com.agenciaviagens.destinos.dto;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padronizado de erro da API.
 * Nao expoe stack trace nem detalhes internos ao cliente.
 */
public record ErroResponse(Instant timestamp,
                           int status,
                           String erro,
                           String mensagem,
                           String caminho,
                           List<CampoInvalido> camposInvalidos) {

    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, caminho, List.of());
    }

    public static ErroResponse de(int status, String erro, String mensagem, String caminho,
                                  List<CampoInvalido> camposInvalidos) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, caminho, camposInvalidos);
    }
}
