package br.com.agenciaviagens.destinos.repository;

import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.util.Textos;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacao volatil da porta de destinos, herdada da primeira versao.
 *
 * ATENCAO: esta classe deixou de ser um bean Spring. Desde a migracao para
 * PostgreSQL, a implementacao usada em producao e DestinoRepositoryJpa; a
 * anotacao @Repository foi removida daqui justamente para que exista um unico
 * candidato a injecao de DestinoRepository.
 *
 * Ela permanece no projeto com dois propositos:
 *
 *   1. servir de dublê nos testes de unidade da camada de servico, que rodam
 *      sem contexto Spring e sem banco -- e continuam passando sem alteracao,
 *      o que e a melhor evidencia de que a troca de persistencia nao vazou
 *      para a regra de negocio;
 *   2. deixar visivel a diferenca entre os dois mundos: aqui os dados vivem em
 *      um mapa e somem no restart, sem transacao, sem constraint e sem
 *      concorrencia real entre processos.
 */
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

    /**
     * Sem banco nao ha lock de linha: a busca para atualizacao e a busca
     * comum. A diferenca de garantia entre as duas implementacoes e real e
     * esta documentada no README.
     */
    @Override
    public Optional<Destino> buscarParaAtualizacao(UUID id) {
        return buscarPorId(id);
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

    @Override
    public boolean vazio() {
        return destinos.isEmpty();
    }
}
