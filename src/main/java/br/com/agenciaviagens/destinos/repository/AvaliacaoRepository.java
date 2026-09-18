package br.com.agenciaviagens.destinos.repository;

import br.com.agenciaviagens.destinos.model.Avaliacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistencia das avaliacoes de destinos.
 */
public interface AvaliacaoRepository {

    Avaliacao salvar(Avaliacao avaliacao);

    Optional<Avaliacao> buscarPorId(UUID id);

    List<Avaliacao> listarPorDestino(UUID destinoId);

    void removerPorDestino(UUID destinoId);
}
