package br.com.agenciaviagens.destinos.repository;

import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.util.Textos;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Implementacao de armazenamento volatil (ConcurrentHashMap).
 * Os dados sao perdidos ao reiniciar a aplicacao, conforme escopo desta etapa.
 */
@Repository
public class DestinoRepositoryEmMemoria implements DestinoRepository {

    private final Map<UUID, Destino> destinos = new ConcurrentHashMap<>();

    @Override
    public Destino salvar(Destino destino) {
        destinos.put(destino.getId(), destino);
        return destino;
    }

    @Override
    public Optional<Destino> buscarPorId(UUID id) {
        return id == null ? Optional.empty() : Optional.ofNullable(destinos.get(id));
    }

    @Override
    public List<Destino> listar(String nome, String localizacao) {
        return destinos.values().stream()
                .filter(destino -> Textos.vazio(nome) || Textos.contem(destino.getNome(), nome))
                .filter(destino -> Textos.vazio(localizacao)
                        || Textos.contem(destino.getLocalizacao().textoPesquisavel(), localizacao))
                .sorted(Comparator.comparing(destino -> Textos.normalizar(destino.getNome())))
                .toList();
    }

    @Override
    public boolean existePorNomeELocalizacao(String nome, String cidade, String pais, UUID idIgnorado) {
        return destinos.values().stream()
                .filter(destino -> idIgnorado == null || !destino.getId().equals(idIgnorado))
                .anyMatch(destino -> Textos.normalizar(destino.getNome()).equals(Textos.normalizar(nome))
                        && Textos.normalizar(destino.getLocalizacao().cidade()).equals(Textos.normalizar(cidade))
                        && Textos.normalizar(destino.getLocalizacao().pais()).equals(Textos.normalizar(pais)));
    }

    @Override
    public boolean remover(UUID id) {
        return id != null && destinos.remove(id) != null;
    }
}
