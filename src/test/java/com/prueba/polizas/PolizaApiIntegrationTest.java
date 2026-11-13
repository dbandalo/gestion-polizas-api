package com.prueba.polizas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolizaApiIntegrationTest {

    private static final String API_KEY = "x-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rechazaPeticionSinApiKey() throws Exception {
        mockMvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listaPolizasFiltradas() throws Exception {
        mockMvc.perform(get("/polizas").header(API_KEY, "123456")
                        .param("tipo", "COLECTIVA").param("estado", "ACTIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("COLECTIVA"));
    }

    @Test
    void tipoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/polizas").header(API_KEY, "123456").param("tipo", "XYZ"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonMalFormadoRetorna400() throws Exception {
        mockMvc.perform(post("/polizas/2/riesgos").header(API_KEY, "123456")
                        .contentType(MediaType.APPLICATION_JSON).content("{malo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agregaRiesgoAColectivaYRetornaId() throws Exception {
        mockMvc.perform(post("/polizas/2/riesgos").header(API_KEY, "123456")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"Apt 303\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void cancelarPolizaCancelaRiesgosEImpideRenovar() throws Exception {
        mockMvc.perform(post("/polizas/2/cancelar").header(API_KEY, "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mockMvc.perform(get("/polizas/2/riesgos").header(API_KEY, "123456"))
                .andExpect(jsonPath("$[0].estado").value("CANCELADO"))
                .andExpect(jsonPath("$[1].estado").value("CANCELADO"));

        mockMvc.perform(post("/polizas/2/renovar").header(API_KEY, "123456"))
                .andExpect(status().isBadRequest());
    }
}
