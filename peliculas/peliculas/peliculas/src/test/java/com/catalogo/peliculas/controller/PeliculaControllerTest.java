package com.catalogo.peliculas.controller;

import com.catalogo.peliculas.exception.PeliculaNoEncontradaException;
import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.service.PeliculaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web: levanta solo el controlador y GlobalExceptionHandler.
 * El servicio es un mock (no hay base de datos), así se prueba que cada caso
 * responde con el código HTTP correcto.
 */
@WebMvcTest(PeliculaController.class)
class PeliculaControllerTest {

    private static final String JSON_VALIDO = """
            {"titulo":"Dune","genero":"Ciencia Ficcion","anio":2021,"actorPrincipal":"Timothee Chalamet"}
            """;

    @Autowired
    private MockMvc mockMvc; // Simula peticiones HTTP sin levantar un servidor real

    @MockitoBean
    private PeliculaService service; // Servicio falso: no se conecta a la BD

    private static Pelicula gladiador() {
        return new Pelicula(1, "Gladiador", "Accion", 2000, "Russell Crowe");
    }

    // ---------- GET /peliculas ----------

    @Test
    void listarRetorna200ConLasPeliculas() throws Exception {
        when(service.listar()).thenReturn(List.of(gladiador(),
                new Pelicula(2, "Interstellar", "Ciencia Ficcion", 2014, "Matthew McConaughey")));

        mockMvc.perform(get("/peliculas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].titulo").value("Gladiador"));
    }

    // ---------- GET /peliculas/{id} ----------

    @Test
    void buscarPorIdExistenteRetorna200() throws Exception {
        when(service.buscarPorId(1)).thenReturn(Optional.of(gladiador()));

        mockMvc.perform(get("/peliculas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.actorPrincipal").value("Russell Crowe"));
    }

    @Test
    void buscarPorIdInexistenteRetorna404ConProblemDetail() throws Exception {
        when(service.buscarPorId(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/peliculas/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail", containsString("id 99")));
    }

    @Test
    void buscarPorIdNoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/peliculas/abc"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /peliculas/titulo/{titulo} ----------

    @Test
    void buscarPorTituloExistenteRetorna200() throws Exception {
        when(service.buscarPorTitulo("gladiador")).thenReturn(Optional.of(gladiador()));

        mockMvc.perform(get("/peliculas/titulo/gladiador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Gladiador"));
    }

    @Test
    void buscarPorTituloInexistenteRetorna404() throws Exception {
        when(service.buscarPorTitulo("noexiste")).thenReturn(Optional.empty());

        mockMvc.perform(get("/peliculas/titulo/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- POST /peliculas/agregar ----------

    @Test
    void crearPeliculaValidaRetorna201ConLocation() throws Exception {
        when(service.guardarPelicula(any(Pelicula.class)))
                .thenReturn(new Pelicula(7, "Dune", "Ciencia Ficcion", 2021, "Timothee Chalamet"));

        mockMvc.perform(post("/peliculas/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/peliculas/7")))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void crearPeliculaConCamposInvalidosRetorna400ConDetalleDeErrores() throws Exception {
        String jsonInvalido = """
                {"titulo":"","genero":"Drama","anio":1800,"actorPrincipal":"Actor"}
                """;

        mockMvc.perform(post("/peliculas/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.anio").exists());

        verify(service, never()).guardarPelicula(any());
    }

    @Test
    void crearPeliculaConJsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/peliculas/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": "))
                .andExpect(status().isBadRequest());
    }

    // ---------- PUT /peliculas/actualizar/{id} ----------

    @Test
    void actualizarPeliculaExistenteRetorna200() throws Exception {
        when(service.actualizarPelicula(eq(1), any(Pelicula.class)))
                .thenReturn(new Pelicula(1, "Dune", "Ciencia Ficcion", 2021, "Timothee Chalamet"));

        mockMvc.perform(put("/peliculas/actualizar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Dune"));
    }

    @Test
    void actualizarPeliculaInexistenteRetorna404() throws Exception {
        when(service.actualizarPelicula(eq(99), any(Pelicula.class)))
                .thenThrow(PeliculaNoEncontradaException.porId(99));

        mockMvc.perform(put("/peliculas/actualizar/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE /peliculas/eliminar/{id} ----------

    @Test
    void eliminarPeliculaExistenteRetorna204() throws Exception {
        mockMvc.perform(delete("/peliculas/eliminar/1"))
                .andExpect(status().isNoContent());

        verify(service).eliminarPorId(1);
    }

    @Test
    void eliminarPeliculaInexistenteRetorna404() throws Exception {
        doThrow(PeliculaNoEncontradaException.porId(99)).when(service).eliminarPorId(99);

        mockMvc.perform(delete("/peliculas/eliminar/99"))
                .andExpect(status().isNotFound());
    }

    // ---------- Error inesperado ----------

    @Test
    void errorInesperadoRetorna500SinExponerDetallesInternos() throws Exception {
        when(service.listar()).thenThrow(new IllegalStateException("conexion perdida con la base de datos"));

        mockMvc.perform(get("/peliculas"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail", not(containsString("conexion perdida"))));
    }
}
