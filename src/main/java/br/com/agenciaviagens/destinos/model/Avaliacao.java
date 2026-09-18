package br.com.agenciaviagens.destinos.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Avaliacao registrada por um viajante para um destino.
 */
public record Avaliacao(UUID id,
                        UUID destinoId,
                        String autor,
                        int nota,
                        String comentario,
                        Instant criadaEm) {

    public static final int NOTA_MINIMA = 1;
    public static final int NOTA_MAXIMA = 5;

    public static Avaliacao nova(UUID destinoId, String autor, int nota, String comentario) {
        return new Avaliacao(UUID.randomUUID(), destinoId, autor, nota, comentario, Instant.now());
    }
}
