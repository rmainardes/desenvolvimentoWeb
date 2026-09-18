package br.com.agenciaviagens.destinos.repository;

import br.com.agenciaviagens.destinos.model.Destino;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistencia de destinos.
 *
 * A interface existe para que a troca do armazenamento em memoria por um
 * banco real (Spring Data JPA, por exemplo) nao afete a camada de servico.
 */
public interface DestinoRepository {

    Destino salvar(Destino destino);

    Optional<Destino> buscarPorId(UUID id);

    List<Destino> listar(String nome, String localizacao);

    boolean existePorNomeELocalizacao(String nome, String cidade, String pais, UUID idIgnorado);

    boolean remover(UUID id);
}
