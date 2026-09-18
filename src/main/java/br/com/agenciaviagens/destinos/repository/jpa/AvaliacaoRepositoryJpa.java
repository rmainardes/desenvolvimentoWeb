package br.com.agenciaviagens.destinos.repository.jpa;

import br.com.agenciaviagens.destinos.model.Avaliacao;
import br.com.agenciaviagens.destinos.repository.AvaliacaoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador entre a porta AvaliacaoRepository e o Spring Data JPA.
 */
@Repository
public class AvaliacaoRepositoryJpa implements AvaliacaoRepository {

    private final AvaliacaoJpaRepository jpa;

    public AvaliacaoRepositoryJpa(AvaliacaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Avaliacao salvar(Avaliacao avaliacao) {
        return jpa.save(avaliacao);
    }

    @Override
    public Optional<Avaliacao> buscarPorId(UUID id) {
        return id == null ? Optional.empty() : jpa.findById(id);
    }

    @Override
    public List<Avaliacao> listarPorDestino(UUID destinoId) {
        return jpa.findByDestinoIdOrderByCriadaEmDesc(destinoId);
    }

    @Override
    public void removerPorDestino(UUID destinoId) {
        jpa.deleteByDestinoId(destinoId);
    }
}
