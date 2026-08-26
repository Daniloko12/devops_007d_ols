package com.catalogo.peliculas.controller;

import com.catalogo.peliculas.service.PeliculaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import com.catalogo.peliculas.model.Pelicula;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PeliculaController.class) // Levanta solamente el Controller.
public class PeliculaControllerTest {

    @Autowired
    private MockMvc mockMvc; //Sirve para simular peticiones HTTP

    @MockitoBean //Crea un servicio falso.No se conecta a BD.No ejecuta lógica real.
    private PeliculaService service;


    @Test
    void listarPeliculas() throws Exception {

    List<Pelicula> peliculas = List.of(
            new Pelicula(
                    1,
                    "Gladiador",
                    "Acción",
                    2000,
                    "Russell Crowe")
    );

    when(service.listar()).thenReturn(peliculas);

    mockMvc.perform(get("/peliculas"))//mockMvc.perform(get("/peliculas"))
            .andExpect(status().isOk());//verifica que el endpoint respondió:
}

}
