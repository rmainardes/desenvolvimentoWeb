package br.com.agenciaviagens.destinos.repository.jpa;

import br.com.agenciaviagens.destinos.model.Avaliacao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data sobre a entidade Avaliacao.
 */
public interface AvaliacaoJpaRepository extends JpaRepository<Avaliacao, UUID> {

    List<Avaliacao> findByDestinoIdOrderByCriadaEmDesc(UUID destinoId);

    void deleteByDestinoId(UUID destinoId);
}
