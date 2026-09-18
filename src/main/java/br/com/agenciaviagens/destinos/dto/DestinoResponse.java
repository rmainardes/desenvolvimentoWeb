package br.com.agenciaviagens.destinos.dto;

import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.model.Localizacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representacao de saida de um destino.
 * Isola o contrato da API da entidade de dominio.
 */
public record DestinoResponse(UUID id,
                              String nome,
                              String descricao,
                              LocalizacaoResponse localizacao,
                              List<String> atividades,
                              int hoteisDisponiveis,
                              BigDecimal precoPacote,
                              BigDecimal notaMedia,
                              int totalAvaliacoes,
                              Instant criadoEm,
                              Instant atualizadoEm) {

    public record LocalizacaoResponse(String cidade, String estado, String pais) {

        static LocalizacaoResponse de(Localizacao localizacao) {
            if (localizacao == null) {
                return null;
            }
            return new LocalizacaoResponse(localizacao.cidade(), localizacao.estado(), localizacao.pais());
        }
    }

    public static DestinoResponse de(Destino destino) {
        return new DestinoResponse(
                destino.getId(),
                destino.getNome(),
                destino.getDescricao(),
                LocalizacaoResponse.de(destino.getLocalizacao()),
                destino.getAtividades(),
                destino.getHoteisDisponiveis(),
                destino.getPrecoPacote(),
                destino.getNotaMedia(),
                destino.getTotalAvaliacoes(),
                destino.getCriadoEm(),
                destino.getAtualizadoEm());
    }
}
