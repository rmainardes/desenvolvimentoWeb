package br.com.agenciaviagens.destinos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.agenciaviagens.destinos.dto.AvaliacaoRequest;
import br.com.agenciaviagens.destinos.dto.DestinoPatchRequest;
import br.com.agenciaviagens.destinos.dto.DestinoRequest;
import br.com.agenciaviagens.destinos.exception.ConflitoDeDadosException;
import br.com.agenciaviagens.destinos.exception.RecursoNaoEncontradoException;
import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.repository.AvaliacaoRepositoryEmMemoria;
import br.com.agenciaviagens.destinos.repository.DestinoRepositoryEmMemoria;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de unidade da camada de servico, sem contexto Spring.
 */
class DestinoServiceTest {

    private DestinoService destinoService;

    @BeforeEach
    void configurar() {
        destinoService = new DestinoService(new DestinoRepositoryEmMemoria(), new AvaliacaoRepositoryEmMemoria());
    }

    private DestinoRequest requisicaoPadrao(String nome, String cidade) {
        return new DestinoRequest(
                nome,
                "Descricao de teste do destino",
                new DestinoRequest.LocalizacaoRequest(cidade, "SC", "Brasil"),
                List.of("Trilha"),
                10,
                new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Cadastra destino e recupera pelo id")
    void cadastraERecupera() {
        Destino criado = destinoService.criar(requisicaoPadrao("Balneario Camboriu", "Balneario Camboriu"));
        Destino encontrado = destinoService.buscarPorId(criado.getId());

        assertEquals("Balneario Camboriu", encontrado.getNome());
        assertEquals(0, encontrado.getTotalAvaliacoes());
        assertEquals(0, BigDecimal.ZERO.compareTo(encontrado.getNotaMedia()));
    }

    @Test
    @DisplayName("Recalcula a media a cada avaliacao registrada")
    void recalculaMedia() {
        Destino destino = destinoService.criar(requisicaoPadrao("Bombinhas", "Bombinhas"));

        destinoService.registrarAvaliacao(destino.getId(), new AvaliacaoRequest("Ana", 5, "Otimo"));
        destinoService.registrarAvaliacao(destino.getId(), new AvaliacaoRequest("Bruno", 4, null));

        Destino aposDuas = destinoService.buscarPorId(destino.getId());
        assertEquals(2, aposDuas.getTotalAvaliacoes());
        assertEquals(0, new BigDecimal("4.50").compareTo(aposDuas.getNotaMedia()));

        destinoService.registrarAvaliacao(destino.getId(), new AvaliacaoRequest("Carla", 3, null));

        Destino aposTres = destinoService.buscarPorId(destino.getId());
        assertEquals(3, aposTres.getTotalAvaliacoes());
        assertEquals(0, new BigDecimal("4.00").compareTo(aposTres.getNotaMedia()));
    }

    @Test
    @DisplayName("Rejeita destino duplicado na mesma cidade e pais")
    void rejeitaDuplicado() {
        destinoService.criar(requisicaoPadrao("Florianopolis", "Florianopolis"));

        assertThrows(ConflitoDeDadosException.class,
                () -> destinoService.criar(requisicaoPadrao("florianopolis", "FLORIANOPOLIS")));
    }

    @Test
    @DisplayName("Lanca excecao ao buscar destino inexistente")
    void destinoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> destinoService.buscarPorId(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Pesquisa ignora acentos e diferenca de maiusculas")
    void pesquisaSemAcento() {
        destinoService.criar(requisicaoPadrao("Sao Joaquim", "Sao Joaquim"));

        assertEquals(1, destinoService.listar("sao joaquim", null).size());
        assertEquals(1, destinoService.listar(null, "BRASIL").size());
        assertTrue(destinoService.listar("cancun", null).isEmpty());
    }

    @Test
    @DisplayName("PATCH altera apenas os campos informados")
    void atualizacaoParcial() {
        Destino destino = destinoService.criar(requisicaoPadrao("Urubici", "Urubici"));

        destinoService.atualizarParcialmente(destino.getId(),
                new DestinoPatchRequest(null, null, null, null, 42, null));

        Destino atualizado = destinoService.buscarPorId(destino.getId());
        assertEquals("Urubici", atualizado.getNome());
        assertEquals(42, atualizado.getHoteisDisponiveis());
        assertEquals(0, new BigDecimal("1000.00").compareTo(atualizado.getPrecoPacote()));
    }

    @Test
    @DisplayName("Excluir destino remove tambem suas avaliacoes")
    void exclusaoRemoveAvaliacoes() {
        Destino destino = destinoService.criar(requisicaoPadrao("Treze Tilias", "Treze Tilias"));
        destinoService.registrarAvaliacao(destino.getId(), new AvaliacaoRequest("Ana", 5, null));

        UUID id = destino.getId();
        destinoService.excluir(id);

        assertThrows(RecursoNaoEncontradoException.class, () -> destinoService.listarAvaliacoes(id));
        assertTrue(destinoService.listar(null, null).isEmpty());
    }
}
