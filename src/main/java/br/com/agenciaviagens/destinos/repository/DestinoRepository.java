package br.com.agenciaviagens.destinos.repository;

import br.com.agenciaviagens.destinos.model.Destino;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistencia de destinos.
 *
 * A interface e a porta que isola a camada de servico do mecanismo de
 * armazenamento. Na primeira versao a implementacao era um ConcurrentHashMap;
 * agora e Spring Data JPA sobre PostgreSQL, e nenhuma linha de regra de
 * negocio precisou mudar por causa da troca -- que era exatamente a aposta
 * feita quando esta interface foi criada.
 */
public interface DestinoRepository {

    Destino salvar(Destino destino);

    Optional<Destino> buscarPorId(UUID id);

    /**
     * Busca o destino reservando-o para escrita.
     *
     * Substitui a trava em memoria que a versao anterior usava para impedir
     * que duas avaliacoes simultaneas recalculassem a media sobre um estado
     * intermediario. Com banco de dados esse controle pertence ao banco: a
     * implementacao JPA aplica um lock pessimista na linha do destino, que so
     * e liberado no fim da transacao.
     */
    Optional<Destino> buscarParaAtualizacao(UUID id);

    List<Destino> listar(String nome, String localizacao);

    boolean existePorNomeELocalizacao(String nome, String cidade, String pais, UUID idIgnorado);

    boolean remover(UUID id);

    /** Usado pela carga de dados iniciais para nao duplicar o exemplo a cada boot. */
    boolean vazio();
}
