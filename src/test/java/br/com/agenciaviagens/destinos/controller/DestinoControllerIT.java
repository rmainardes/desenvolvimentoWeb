package br.com.agenciaviagens.destinos.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de integracao do fluxo HTTP completo contra um PostgreSQL real.
 *
 * Sobre o nome da classe: o sufixo IT existe desde a primeira versao, mas ate
 * agora nenhum plugin a executava -- o surefire ignora esse padrao. Com o
 * maven-failsafe-plugin configurado no pom, esta classe roda em "./mvnw verify".
 * Requer Docker em execucao.
 *
 * O banco vem de um container descartavel, e nao de um banco em memoria: as
 * migrations do Flyway usam tipos do PostgreSQL (uuid, timestamp with time
 * zone, numeric) e o mapeamento e validado contra elas na subida. Testar em
 * outro dialeto daria uma confianca falsa.
 */
@Testcontainers
@SpringBootTest(properties = {
        "app.dados-iniciais=false",
        "app.usuarios-iniciais.admin-username=admin-teste",
        "app.usuarios-iniciais.admin-senha=senha-admin-teste",
        "app.usuarios-iniciais.user-username=viajante-teste",
        "app.usuarios-iniciais.user-senha=senha-user-teste"
})
@AutoConfigureMockMvc
class DestinoControllerIT {

    private static final String ADMIN = "admin-teste";
    private static final String SENHA_ADMIN = "senha-admin-teste";
    private static final String USER = "viajante-teste";
    private static final String SENHA_USER = "senha-user-teste";

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("destinos_teste")
            .withUsername("destinos")
            .withPassword("destinos");

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    private static final String DESTINO_JSON = """
            {
              "nome": "Jericoacoara",
              "descricao": "Vila de pescadores com dunas, lagoas e por do sol na Duna do Por do Sol.",
              "localizacao": { "cidade": "Jijoca de Jericoacoara", "estado": "CE", "pais": "Brasil" },
              "atividades": ["Kitesurf", "Passeio de buggy"],
              "hoteisDisponiveis": 60,
              "precoPacote": 3200.00
            }
            """;

    // ------------------------------------------------------------------
    // Fluxo funcional
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Fluxo completo: cadastro, consulta, avaliacao e exclusao")
    void fluxoCompleto() throws Exception {
        String corpoCriado = mockMvc.perform(post("/api/v1/destinos")
                        .with(httpBasic(ADMIN, SENHA_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DESTINO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(corpoCriado, "$.id");

        // Consulta e publica: sem credencial.
        mockMvc.perform(get("/api/v1/destinos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Jericoacoara"))
                .andExpect(jsonPath("$.atividades.length()").value(2));

        mockMvc.perform(get("/api/v1/destinos").param("localizacao", "ceara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/destinos").param("nome", "jeri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Avaliar exige autenticacao, mas nao exige ser ADMIN.
        mockMvc.perform(post("/api/v1/destinos/{id}/avaliacoes", id)
                        .with(httpBasic(USER, SENHA_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Ana\",\"nota\":5,\"comentario\":\"Inesquecivel\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumoDestino.totalAvaliacoes").value(1))
                .andExpect(jsonPath("$.resumoDestino.notaMedia").exists());

        mockMvc.perform(delete("/api/v1/destinos/{id}", id)
                        .with(httpBasic(ADMIN, SENHA_ADMIN)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/destinos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Dados persistem de verdade: o destino sobrevive a uma nova consulta")
    void persistenciaEfetiva() throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/destinos")
                        .with(httpBasic(ADMIN, SENHA_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DESTINO_JSON.replace("Jericoacoara", "Lencois Maranhenses")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(corpo, "$.id");

        mockMvc.perform(get("/api/v1/destinos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criadoEm").exists());

        mockMvc.perform(delete("/api/v1/destinos/{id}", id).with(httpBasic(ADMIN, SENHA_ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Cadastro duplicado no mesmo nome, cidade e pais retorna 409")
    void cadastroDuplicado() throws Exception {
        String json = DESTINO_JSON.replace("Jericoacoara", "Porto de Galinhas");

        String corpo = mockMvc.perform(post("/api/v1/destinos")
                        .with(httpBasic(ADMIN, SENHA_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/destinos")
                        .with(httpBasic(ADMIN, SENHA_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        String id = JsonPath.read(corpo, "$.id");
        mockMvc.perform(delete("/api/v1/destinos/{id}", id).with(httpBasic(ADMIN, SENHA_ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Cadastro com payload invalido retorna 400 com a lista de campos")
    void cadastroInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/destinos")
                        .with(httpBasic(ADMIN, SENHA_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"hoteisDisponiveis\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.camposInvalidos.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    // ------------------------------------------------------------------
    // Autenticacao e autorizacao
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Consulta do catalogo e publica")
    void consultaPublica() throws Exception {
        mockMvc.perform(get("/api/v1/destinos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Escrita sem credencial retorna 401 no formato de erro da API")
    void escritaSemCredencial() throws Exception {
        mockMvc.perform(post("/api/v1/destinos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DESTINO_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("Credencial invalida retorna 401")
    void credencialInvalida() throws Exception {
        mockMvc.perform(post("/api/v1/destinos")
                        .with(httpBasic(ADMIN, "senha-errada"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DESTINO_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER autenticado nao cadastra destino: 403")
    void usuarioComumNaoCadastra() throws Exception {
        mockMvc.perform(post("/api/v1/destinos")
                        .with(httpBasic(USER, SENHA_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DESTINO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("USER autenticado nao exclui destino: 403")
    void usuarioComumNaoExclui() throws Exception {
        mockMvc.perform(delete("/api/v1/destinos/{id}", "00000000-0000-0000-0000-000000000000")
                        .with(httpBasic(USER, SENHA_USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Avaliacao sem credencial retorna 401")
    void avaliacaoExigeAutenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/destinos/{id}/avaliacoes", "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Ana\",\"nota\":5}"))
                .andExpect(status().isUnauthorized());
    }
}
