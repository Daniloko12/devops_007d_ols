package com.catalogo.peliculas;

import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.repository.PeliculaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la carga inicial segura frente a varias réplicas: tabla vacía, tabla con datos,
 * limpieza de duplicados, película ya insertada por otra réplica y choque con la
 * restricción única del título.
 */
class DataLoaderTest {

    @Test
    void cargaLasPeliculasInicialesCuandoLaTablaEstaVacia() throws Exception {
        PeliculaRepository repository = mock(PeliculaRepository.class);
        when(repository.count()).thenReturn(0L);

        new DataLoader().init(repository).run();

        verify(repository, times(6)).save(any(Pelicula.class));
        verify(repository, never()).deleteAll(anyIterable());
    }

    @Test
    void noCargaNiEliminaCuandoLaTablaTieneDatosSinDuplicados() throws Exception {
        PeliculaRepository repository = mock(PeliculaRepository.class);
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(
                new Pelicula(1, "Gladiador", "Acción", 2000, "Russell Crowe"),
                new Pelicula(2, "Avengers", "Acción", 2012, "Robert Downey Jr")));
        when(repository.count()).thenReturn(2L);

        new DataLoader().init(repository).run();

        verify(repository, never()).save(any(Pelicula.class));
        verify(repository, never()).deleteAll(anyIterable());
    }

    @Test
    void eliminaLosTitulosDuplicadosConservandoElMasAntiguo() throws Exception {
        PeliculaRepository repository = mock(PeliculaRepository.class);
        Pelicula original = new Pelicula(1, "Gladiador", "Acción", 2000, "Russell Crowe");
        Pelicula duplicado = new Pelicula(7, "gladiador", "Acción", 2000, "Russell Crowe");
        Pelicula unica = new Pelicula(3, "Avengers", "Acción", 2012, "Robert Downey Jr");
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(original, unica, duplicado));
        when(repository.count()).thenReturn(2L);

        new DataLoader().init(repository).run();

        verify(repository).deleteAll(List.of(duplicado));
        verify(repository, never()).save(any(Pelicula.class));
    }

    @Test
    void noInsertaLaPeliculaQueOtraReplicaYaCargo() throws Exception {
        PeliculaRepository repository = mock(PeliculaRepository.class);
        when(repository.count()).thenReturn(0L);
        when(repository.existsByTituloIgnoreCase("Gladiador")).thenReturn(true);

        new DataLoader().init(repository).run();

        verify(repository, times(5)).save(any(Pelicula.class));
        verify(repository, never()).save(argThat((Pelicula p) -> "Gladiador".equals(p.getTitulo())));
    }

    @Test
    void continuaSiLaRestriccionUnicaRechazaUnaPelicula() {
        PeliculaRepository repository = mock(PeliculaRepository.class);
        when(repository.count()).thenReturn(0L);
        when(repository.save(argThat((Pelicula p) -> p != null && "Avengers".equals(p.getTitulo()))))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'Avengers'"));

        assertThatNoException().isThrownBy(() -> new DataLoader().init(repository).run());

        verify(repository, times(6)).save(any(Pelicula.class));
    }
}
