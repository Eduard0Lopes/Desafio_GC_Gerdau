package com.gerdau.desafio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira com o seeder ligado contra um MongoDB em container,
 * importando os JSONs reais gerados pelo pipeline de IA.
 */
@Testcontainers
@SpringBootTest(properties = "app.seed.enabled=true")
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    WebApplicationContext context;

    @Autowired
    MongoTemplate mongoTemplate;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void seederImportaOsArtefatosDaIa() throws Exception {
        mvc.perform(get("/api/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gruposServico").value(3))
                .andExpect(jsonPath("$.codigosServico").value(7332))
                .andExpect(jsonPath("$.codigosCompletos").value(9327));
    }

    @Test
    void codigoCompletoMigradoVemEnriquecido() throws Exception {
        mvc.perform(get("/api/codigos-completos/legado/4033776"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00100100000115"))
                .andExpect(jsonPath("$[0].grupoNome").value("Civil"))
                .andExpect(jsonPath("$[0].unidadeSigla").value("SV"))
                .andExpect(jsonPath("$[0].servicoDescricao").value("SV-EQUIPE TELHADO"));
    }

    @Test
    void similaridadeUsaOMesmoScoreDoPython() throws Exception {
        mvc.perform(get("/api/codigos-servico/similares").param("descricao", "SV EQUIPE DE TELHADO").param("limite", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("000001"))
                .andExpect(jsonPath("$[0].score").value(0.8514))
                .andExpect(jsonPath("$[0].status").value("ALERTA"));
    }

    @Test
    void fluxoCompletoDeCadastroComVinculos() throws Exception {
        String grupoId = idDe(json(post("/api/grupos-servico"), "{\"nome\":\"Grupo Teste IT\"}").andExpect(status().isCreated()));
        json(post("/api/grupos-servico"), "{\"nome\":\"grupo teste it\"}").andExpect(status().isConflict());

        String fornecedorId = idDe(json(post("/api/codigos-fornecedor"), "{\"codigoFornecedorSAP\":999000111,\"nome\":\"Fornecedor IT\"}")
                .andExpect(status().isCreated()));
        String unidadeId = idDe(json(post("/api/unidades-medida"), "{\"sigla\":\"pct\",\"nome\":\"Pacote\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sigla").value("PCT")));

        String body = String.format("{\"gsId\":\"%s\",\"cfId\":\"%s\",\"umId\":\"%s\",\"descricaoServico\":\"SV TESTE INTEGRACAO XPTO\"}",
                grupoId, fornecedorId, unidadeId);
        json(post("/api/codigos-completos"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(startsWith(grupoId + fornecedorId)))
                .andExpect(jsonPath("$.descricaoSnapshot").value("Grupo Teste IT | Fornecedor 999000111 | SV TESTE INTEGRACAO XPTO | PCT"));

        mvc.perform(delete("/api/grupos-servico/" + grupoId)).andExpect(status().isConflict());
        mvc.perform(get("/api/codigos-completos").param("gsId", grupoId))
                .andExpect(jsonPath("$.totalElements").value(1));
        json(put("/api/grupos-servico/" + grupoId), "{\"nome\":\"Grupo Teste IT 2\"}")
                .andExpect(jsonPath("$.nomeNormalizado").value("grupo teste it 2"));
    }

    @Test
    void duplicidadeExigeJustificativa() throws Exception {
        json(post("/api/codigos-servico"), "{\"descricao\":\"DIARIA RESGATISTA NIVEL 3 ATE 8H X\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(startsWith("Provável duplicidade com o código")));
        json(post("/api/codigos-servico"), "{\"descricao\":\"DIARIA RESGATISTA NIVEL 3 ATE 8H X\",\"justificativa\":\"Plural é outro escopo\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("SISTEMA"));
    }

    @Test
    void errosDeValidacaoRetornamCampos() throws Exception {
        json(post("/api/codigos-servico"), "{\"descricao\":\"" + "X".repeat(41) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists());
        mvc.perform(get("/api/codigos-servico").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/grupos-servico/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/unidades-medida").param("q", "metro"))
                .andExpect(jsonPath("$[*].sigla", hasItem("M")));
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                               String body) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String idDe(ResultActions result) throws Exception {
        String content = result.andReturn().getResponse().getContentAsString();
        return content.replaceAll("(?s).*\"id\":\"(\\d+)\".*", "$1");
    }
}
