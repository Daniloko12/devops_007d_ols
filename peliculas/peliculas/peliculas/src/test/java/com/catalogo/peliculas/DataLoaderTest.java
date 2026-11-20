package com.catalogo.peliculas;

import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.repository.PeliculaRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre las dos ramas de DataLoader: tabla vacía (carga datos) y tabla con datos (no hace nada). */
class DataLoaderTest {

    @Test
    void cargaLasPeliculasInicialesCuandoLaTablaEstaVacia() throws Exception {
        PeliculaRepository repository = mock(PeliculaRepository.class);
        when(repository.count()).thenReturn(0L);

        new DataLoader().init(repository).run();

        verify(repository, times(6)).save(any(Pelicula.class));
    }

    @Test
    void noDuplicaDatosCuandoLaTablaYaTieneRegistros() throws Exception {
        PeliculaRepository repository = mock(PeliculaRepository.class);
        when(repository.count()).thenReturn(6L);

        new DataLoader().init(repository).run();

        verify(repository, never()).save(any(Pelicula.class));
    }
}
