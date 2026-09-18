package br.com.agenciaviagens.destinos.dto;

import br.com.agenciaviagens.destinos.model.Avaliacao;
import br.com.agenciaviagens.destinos.model.Destino;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representacao de saida de uma avaliacao.
 * Inclui o resumo atualizado do destino para que o cliente enxergue
 * imediatamente o efeito da avaliacao sobre a media.
 */
public record AvaliacaoResponse(UUID id,
                                UUID destinoId,
                                String autor,
                                int nota,
                                String comentario,
                                Instant criadaEm,
                                ResumoDestino resumoDestino) {

    public record ResumoDestino(BigDecimal notaMedia, int totalAvaliacoes) {
    }

    public static AvaliacaoResponse de(Avaliacao avaliacao, Destino destino) {
        ResumoDestino resumo = destino == null
                ? null
                : new ResumoDestino(destino.getNotaMedia(), destino.getTotalAvaliacoes());
        return new AvaliacaoResponse(
                avaliacao.id(),
                avaliacao.destinoId(),
                avaliacao.autor(),
                avaliacao.nota(),
                avaliacao.comentario(),
                avaliacao.criadaEm(),
                resumo);
    }

    public static AvaliacaoResponse de(Avaliacao avaliacao) {
        return de(avaliacao, null);
    }
}
