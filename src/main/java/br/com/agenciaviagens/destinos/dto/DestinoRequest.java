package br.com.agenciaviagens.destinos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Payload de entrada para cadastro (POST) e substituicao (PUT) de destinos.
 * Todos os campos sao obrigatorios: PUT substitui o recurso por completo.
 */
public record DestinoRequest(

        @NotBlank(message = "O nome do destino e obrigatorio")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
        String nome,

        @NotBlank(message = "A descricao e obrigatoria")
        @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres")
        String descricao,

        @NotNull(message = "A localizacao e obrigatoria")
        @Valid
        LocalizacaoRequest localizacao,

        @Size(max = 20, message = "Informe no maximo 20 atividades")
        List<@NotBlank(message = "A atividade nao pode ser vazia")
             @Size(max = 80, message = "Cada atividade deve ter no maximo 80 caracteres") String> atividades,

        @NotNull(message = "Informe a quantidade de hoteis disponiveis")
        @PositiveOrZero(message = "A quantidade de hoteis nao pode ser negativa")
        Integer hoteisDisponiveis,

        @NotNull(message = "O preco do pacote e obrigatorio")
        @DecimalMin(value = "0.00", message = "O preco do pacote nao pode ser negativo")
        @Digits(integer = 9, fraction = 2, message = "O preco deve ter no maximo 9 inteiros e 2 decimais")
        BigDecimal precoPacote) {

    public record LocalizacaoRequest(

            @NotBlank(message = "A cidade e obrigatoria")
            @Size(max = 120, message = "A cidade deve ter no maximo 120 caracteres")
            String cidade,

            @Size(max = 120, message = "O estado deve ter no maximo 120 caracteres")
            String estado,

            @NotBlank(message = "O pais e obrigatorio")
            @Size(max = 120, message = "O pais deve ter no maximo 120 caracteres")
            String pais) {
    }
}
