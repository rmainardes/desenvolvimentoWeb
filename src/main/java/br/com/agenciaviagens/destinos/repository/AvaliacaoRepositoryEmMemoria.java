package br.com.agenciaviagens.destinos.repository;

import br.com.agenciaviagens.destinos.model.Avaliacao;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Implementacao de armazenamento volatil das avaliacoes.
 */
@Repository
public class AvaliacaoRepositoryEmMemoria implements AvaliacaoRepository {

    private final Map<UUID, Avaliacao> avaliacoes = new ConcurrentHashMap<>();

    @Override
    public Avaliacao salvar(Avaliacao avaliacao) {
        avaliacoes.put(avaliacao.id(), avaliacao);
        return avaliacao;
    }

    @Override
    public Optional<Avaliacao> buscarPorId(UUID id) {
        return id == null ? Optional.empty() : Optional.ofNullable(avaliacoes.get(id));
    }

    @Override
    public List<Avaliacao> listarPorDestino(UUID destinoId) {
        return avaliacoes.values().stream()
                .filter(avaliacao -> avaliacao.destinoId().equals(destinoId))
                .sorted(Comparator.comparing(Avaliacao::criadaEm).reversed())
                .toList();
    }

    @Override
    public void removerPorDestino(UUID destinoId) {
        avaliacoes.values().removeIf(avaliacao -> avaliacao.destinoId().equals(destinoId));
    }
}
