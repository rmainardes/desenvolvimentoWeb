package br.com.agenciaviagens.destinos.controller;

import br.com.agenciaviagens.destinos.dto.AvaliacaoRequest;
import br.com.agenciaviagens.destinos.dto.AvaliacaoResponse;
import br.com.agenciaviagens.destinos.service.DestinoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST das avaliacoes, modeladas como sub-recurso de um destino.
 */
@RestController
@RequestMapping("/api/v1/destinos/{destinoId}/avaliacoes")
public class AvaliacaoController {

    private final DestinoService destinoService;

    public AvaliacaoController(DestinoService destinoService) {
        this.destinoService = destinoService;
    }

    /** Registra uma avaliacao e devolve a nova media do destino. */
    @PostMapping
    public ResponseEntity<AvaliacaoResponse> registrar(@PathVariable UUID destinoId,
                                                       @Valid @RequestBody AvaliacaoRequest request) {
        DestinoService.ResultadoAvaliacao resultado = destinoService.registrarAvaliacao(destinoId, request);
        AvaliacaoResponse response = AvaliacaoResponse.de(resultado.avaliacao(), resultado.destino());
        URI local = URI.create("/api/v1/destinos/" + destinoId + "/avaliacoes/" + response.id());
        return ResponseEntity.created(local).body(response);
    }

    /** Lista as avaliacoes de um destino, da mais recente para a mais antiga. */
    @GetMapping
    public ResponseEntity<List<AvaliacaoResponse>> listar(@PathVariable UUID destinoId) {
        List<AvaliacaoResponse> avaliacoes = destinoService.listarAvaliacoes(destinoId).stream()
                .map(AvaliacaoResponse::de)
                .toList();
        return ResponseEntity.ok(avaliacoes);
    }

    /** Detalha uma avaliacao especifica. */
    @GetMapping("/{avaliacaoId}")
    public ResponseEntity<AvaliacaoResponse> detalhar(@PathVariable UUID destinoId,
                                                      @PathVariable UUID avaliacaoId) {
        return ResponseEntity.ok(AvaliacaoResponse.de(destinoService.buscarAvaliacao(destinoId, avaliacaoId)));
    }
}
