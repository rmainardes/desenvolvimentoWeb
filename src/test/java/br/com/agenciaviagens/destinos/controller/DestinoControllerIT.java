package br.com.agenciaviagens.destinos.controller;

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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de integracao do fluxo HTTP completo, com a base em memoria vazia.
 */
@SpringBootTest(properties = "app.dados-iniciais=false")
@AutoConfigureMockMvc
class DestinoControllerIT {

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

    @Test
    @DisplayName("Fluxo completo: cadastro, consulta, avaliacao e exclusao")
    void fluxoCompleto() throws Exception {
        String corpoCriado = mockMvc.perform(post("/api/v1/destinos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DESTINO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(corpoCriado, "$.id");

        mockMvc.perform(get("/api/v1/destinos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Jericoacoara"));

        mockMvc.perform(get("/api/v1/destinos").param("localizacao", "ceara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/destinos").param("nome", "jeri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(post("/api/v1/destinos/{id}/avaliacoes", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Ana\",\"nota\":5,\"comentario\":\"Inesquecivel\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumoDestino.totalAvaliacoes").value(1));

        mockMvc.perform(delete("/api/v1/destinos/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/destinos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Cadastro com payload invalido retorna 400 com a lista de campos")
    void cadastroInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/destinos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"hoteisDisponiveis\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.camposInvalidos.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
