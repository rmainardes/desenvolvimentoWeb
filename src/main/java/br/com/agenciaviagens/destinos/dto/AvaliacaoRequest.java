package br.com.agenciaviagens.destinos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de registro de avaliacao de um destino.
 */
public record AvaliacaoRequest(

        @NotBlank(message = "Informe o autor da avaliacao")
        @Size(max = 80, message = "O autor deve ter no maximo 80 caracteres")
        String autor,

        @NotNull(message = "A nota e obrigatoria")
        @Min(value = 1, message = "A nota minima e 1")
        @Max(value = 5, message = "A nota maxima e 5")
        Integer nota,

        @Size(max = 500, message = "O comentario deve ter no maximo 500 caracteres")
        String comentario) {
}
