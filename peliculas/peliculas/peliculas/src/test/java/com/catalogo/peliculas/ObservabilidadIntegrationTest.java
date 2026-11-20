package com.catalogo.peliculas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración: levanta la aplicación completa con H2 y verifica
 * 1) los endpoints de observabilidad que usarán Kubernetes y Prometheus, y
 * 2) un flujo real crear -> consultar -> eliminar contra la base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilidadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReportaUpIncluyendoLaBaseDeDatos() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void probesDeKubernetesEstanDisponibles() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void infoExponeNombreYCommitParaTrazabilidad() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("peliculas"))
                .andExpect(jsonPath("$.app.commit").value("local"));
    }

    @Test
    void flujoCompletoCrearConsultarYEliminarContraH2() throws Exception {
        String json = """
                {"titulo":"Matrix","genero":"Ciencia Ficcion","anio":1999,"actorPrincipal":"Keanu Reeves"}
                """;

        String ubicacion = mockMvc.perform(post("/peliculas/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(ubicacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Matrix"));

        String id = ubicacion.substring(ubicacion.lastIndexOf('/') + 1);
        mockMvc.perform(delete("/peliculas/eliminar/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ubicacion))
                .andExpect(status().isNotFound());
    }
}
