package br.com.agenciaviagens.destinos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Payload de atualizacao parcial (PATCH).
 * Campos nulos sao ignorados; campos informados sao validados e aplicados.
 * A localizacao, quando informada, e substituida por completo.
 */
public record DestinoPatchRequest(

        @NotBlank(message = "O nome nao pode ser vazio quando informado")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
        String nome,

        @NotBlank(message = "A descricao nao pode ser vazia quando informada")
        @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres")
        String descricao,

        @Valid
        DestinoRequest.LocalizacaoRequest localizacao,

        @Size(max = 20, message = "Informe no maximo 20 atividades")
        List<@NotBlank(message = "A atividade nao pode ser vazia")
             @Size(max = 80, message = "Cada atividade deve ter no maximo 80 caracteres") String> atividades,

        @PositiveOrZero(message = "A quantidade de hoteis nao pode ser negativa")
        Integer hoteisDisponiveis,

        @DecimalMin(value = "0.00", message = "O preco do pacote nao pode ser negativo")
        @Digits(integer = 9, fraction = 2, message = "O preco deve ter no maximo 9 inteiros e 2 decimais")
        BigDecimal precoPacote) {

    public boolean vazio() {
        return nome == null && descricao == null && localizacao == null
                && atividades == null && hoteisDisponiveis == null && precoPacote == null;
    }
}
