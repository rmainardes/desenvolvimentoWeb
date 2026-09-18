package br.com.agenciaviagens.destinos.controller;

import br.com.agenciaviagens.destinos.dto.DestinoPatchRequest;
import br.com.agenciaviagens.destinos.dto.DestinoRequest;
import br.com.agenciaviagens.destinos.dto.DestinoResponse;
import br.com.agenciaviagens.destinos.service.DestinoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de destinos turisticos.
 * Responsabilidade unica: traduzir HTTP para a camada de servico e de volta.
 */
@RestController
@RequestMapping("/api/v1/destinos")
public class DestinoController {

    private final DestinoService destinoService;

    public DestinoController(DestinoService destinoService) {
        this.destinoService = destinoService;
    }

    /**
     * Lista e pesquisa destinos.
     * Sem parametros retorna todos; com "nome" e/ou "localizacao" filtra
     * (comparacao parcial, sem diferenciar acentos ou maiusculas).
     */
    @GetMapping
    public ResponseEntity<List<DestinoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String localizacao) {

        List<DestinoResponse> destinos = destinoService.listar(nome, localizacao).stream()
                .map(DestinoResponse::de)
                .toList();
        return ResponseEntity.ok(destinos);
    }

    /** Detalha um destino especifico. */
    @GetMapping("/{id}")
    public ResponseEntity<DestinoResponse> detalhar(@PathVariable UUID id) {
        return ResponseEntity.ok(DestinoResponse.de(destinoService.buscarPorId(id)));
    }

    /** Cadastra um destino. */
    @PostMapping
    public ResponseEntity<DestinoResponse> cadastrar(@Valid @RequestBody DestinoRequest request) {
        DestinoResponse response = DestinoResponse.de(destinoService.criar(request));
        return ResponseEntity.created(URI.create("/api/v1/destinos/" + response.id())).body(response);
    }

    /** Substitui todos os dados cadastrais de um destino. */
    @PutMapping("/{id}")
    public ResponseEntity<DestinoResponse> substituir(@PathVariable UUID id,
                                                      @Valid @RequestBody DestinoRequest request) {
        return ResponseEntity.ok(DestinoResponse.de(destinoService.substituir(id, request)));
    }

    /** Atualiza parcialmente os dados de um destino. */
    @PatchMapping("/{id}")
    public ResponseEntity<DestinoResponse> atualizarParcialmente(
            @PathVariable UUID id,
            @Valid @RequestBody DestinoPatchRequest request) {
        return ResponseEntity.ok(DestinoResponse.de(destinoService.atualizarParcialmente(id, request)));
    }

    /** Exclui um destino e suas avaliacoes. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        destinoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
