package br.com.agenciaviagens.destinos.service;

import br.com.agenciaviagens.destinos.dto.AvaliacaoRequest;
import br.com.agenciaviagens.destinos.dto.DestinoPatchRequest;
import br.com.agenciaviagens.destinos.dto.DestinoRequest;
import br.com.agenciaviagens.destinos.exception.ConflitoDeDadosException;
import br.com.agenciaviagens.destinos.exception.RecursoNaoEncontradoException;
import br.com.agenciaviagens.destinos.exception.RequisicaoInvalidaException;
import br.com.agenciaviagens.destinos.model.Avaliacao;
import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.model.Localizacao;
import br.com.agenciaviagens.destinos.repository.AvaliacaoRepository;
import br.com.agenciaviagens.destinos.repository.DestinoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Camada de servico: concentra as regras de negocio de destinos e avaliacoes.
 * Os controllers apenas traduzem HTTP para chamadas desta classe.
 */
@Service
public class DestinoService {

    private static final Logger log = LoggerFactory.getLogger(DestinoService.class);

    private final DestinoRepository destinoRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    /**
     * Uma trava por destino: garante que duas avaliacoes simultaneas do mesmo
     * destino nao recalculem a media em cima de um estado intermediario.
     * Em um cenario com banco de dados, isso seria uma transacao.
     */
    private final ConcurrentMap<UUID, Object> travas = new ConcurrentHashMap<>();

    public DestinoService(DestinoRepository destinoRepository, AvaliacaoRepository avaliacaoRepository) {
        this.destinoRepository = destinoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    /** Lista destinos, opcionalmente filtrando por nome e/ou localizacao. */
    public List<Destino> listar(String nome, String localizacao) {
        return destinoRepository.listar(nome, localizacao);
    }

    /** Busca um destino pelo identificador ou lanca 404. */
    public Destino buscarPorId(UUID id) {
        return destinoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Destino nao encontrado: " + id));
    }

    /** Cadastra um novo destino. */
    public Destino criar(DestinoRequest request) {
        Localizacao localizacao = paraLocalizacao(request.localizacao());
        validarDuplicidade(request.nome(), localizacao, null);

        Destino destino = new Destino(
                request.nome().trim(),
                request.descricao().trim(),
                localizacao,
                request.atividades() == null ? List.of() : request.atividades(),
                request.hoteisDisponiveis(),
                normalizarPreco(request.precoPacote()));

        destinoRepository.salvar(destino);
        log.info("Destino cadastrado: id={}", destino.getId());
        return destino;
    }

    /** Substitui todos os dados cadastrais de um destino (PUT). */
    public Destino substituir(UUID id, DestinoRequest request) {
        Destino destino = buscarPorId(id);
        Localizacao localizacao = paraLocalizacao(request.localizacao());
        validarDuplicidade(request.nome(), localizacao, id);

        destino.atualizarDados(
                request.nome().trim(),
                request.descricao().trim(),
                localizacao,
                request.atividades() == null ? List.of() : request.atividades(),
                request.hoteisDisponiveis(),
                normalizarPreco(request.precoPacote()));

        destinoRepository.salvar(destino);
        log.info("Destino atualizado (PUT): id={}", id);
        return destino;
    }

    /** Atualiza apenas os campos informados (PATCH). */
    public Destino atualizarParcialmente(UUID id, DestinoPatchRequest request) {
        if (request == null || request.vazio()) {
            throw new RequisicaoInvalidaException("Informe ao menos um campo para atualizacao parcial");
        }

        Destino destino = buscarPorId(id);

        String nome = request.nome() == null ? destino.getNome() : request.nome().trim();
        String descricao = request.descricao() == null ? destino.getDescricao() : request.descricao().trim();
        Localizacao localizacao = request.localizacao() == null
                ? destino.getLocalizacao()
                : paraLocalizacao(request.localizacao());
        List<String> atividades = request.atividades() == null ? destino.getAtividades() : request.atividades();
        int hoteis = request.hoteisDisponiveis() == null
                ? destino.getHoteisDisponiveis()
                : request.hoteisDisponiveis();
        BigDecimal preco = request.precoPacote() == null
                ? destino.getPrecoPacote()
                : normalizarPreco(request.precoPacote());

        validarDuplicidade(nome, localizacao, id);
        destino.atualizarDados(nome, descricao, localizacao, atividades, hoteis, preco);

        destinoRepository.salvar(destino);
        log.info("Destino atualizado (PATCH): id={}", id);
        return destino;
    }

    /** Exclui o destino e as avaliacoes associadas. */
    public void excluir(UUID id) {
        Destino destino = buscarPorId(id);
        avaliacaoRepository.removerPorDestino(destino.getId());
        destinoRepository.remover(destino.getId());
        travas.remove(destino.getId());
        log.info("Destino excluido: id={}", id);
    }

    /**
     * Registra uma avaliacao e recalcula a nota media do destino a partir de
     * todas as avaliacoes existentes.
     */
    public ResultadoAvaliacao registrarAvaliacao(UUID destinoId, AvaliacaoRequest request) {
        Destino destino = buscarPorId(destinoId);
        Object trava = travas.computeIfAbsent(destino.getId(), chave -> new Object());

        synchronized (trava) {
            Avaliacao avaliacao = avaliacaoRepository.salvar(Avaliacao.nova(
                    destino.getId(),
                    request.autor().trim(),
                    request.nota(),
                    request.comentario() == null ? null : request.comentario().trim()));

            recalcularResumo(destino);
            destinoRepository.salvar(destino);
            log.info("Avaliacao registrada: destinoId={} novaMedia={}", destinoId, destino.getNotaMedia());
            return new ResultadoAvaliacao(avaliacao, destino);
        }
    }

    /** Lista as avaliacoes de um destino (o destino precisa existir). */
    public List<Avaliacao> listarAvaliacoes(UUID destinoId) {
        Destino destino = buscarPorId(destinoId);
        return avaliacaoRepository.listarPorDestino(destino.getId());
    }

    /** Busca uma avaliacao especifica de um destino. */
    public Avaliacao buscarAvaliacao(UUID destinoId, UUID avaliacaoId) {
        Destino destino = buscarPorId(destinoId);
        return avaliacaoRepository.buscarPorId(avaliacaoId)
                .filter(avaliacao -> avaliacao.destinoId().equals(destino.getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Avaliacao nao encontrada para o destino informado: " + avaliacaoId));
    }

    private void recalcularResumo(Destino destino) {
        List<Avaliacao> avaliacoes = avaliacaoRepository.listarPorDestino(destino.getId());
        if (avaliacoes.isEmpty()) {
            destino.atualizarResumoAvaliacoes(BigDecimal.ZERO, 0);
            return;
        }
        BigDecimal soma = avaliacoes.stream()
                .map(avaliacao -> BigDecimal.valueOf(avaliacao.nota()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal media = soma.divide(BigDecimal.valueOf(avaliacoes.size()),
                Destino.ESCALA_NOTA, RoundingMode.HALF_UP);
        destino.atualizarResumoAvaliacoes(media, avaliacoes.size());
    }

    private void validarDuplicidade(String nome, Localizacao localizacao, UUID idIgnorado) {
        boolean duplicado = destinoRepository.existePorNomeELocalizacao(
                nome, localizacao.cidade(), localizacao.pais(), idIgnorado);
        if (duplicado) {
            throw new ConflitoDeDadosException(
                    "Ja existe um destino com o nome '" + nome + "' em " + localizacao.cidade()
                            + " (" + localizacao.pais() + ")");
        }
    }

    private Localizacao paraLocalizacao(DestinoRequest.LocalizacaoRequest request) {
        return new Localizacao(request.cidade(), request.estado(), request.pais());
    }

    private BigDecimal normalizarPreco(BigDecimal preco) {
        return preco.setScale(2, RoundingMode.HALF_UP);
    }

    /** Resultado do registro de avaliacao: a avaliacao criada e o destino ja atualizado. */
    public record ResultadoAvaliacao(Avaliacao avaliacao, Destino destino) {
    }
}
