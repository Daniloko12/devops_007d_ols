package com.catalogo.peliculas.service;

import com.catalogo.peliculas.exception.PeliculaNoEncontradaException;
import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.repository.PeliculaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la lógica de negocio. El repositorio es un mock (sin BD)
 * y las métricas se registran en un SimpleMeterRegistry en memoria para poder
 * verificar que cada operación incrementa su contador.
 */
@ExtendWith(MockitoExtension.class)
class PeliculaServiceTest {

    @Mock
    private PeliculaRepository repository;

    private SimpleMeterRegistry meterRegistry;
    private PeliculaService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new PeliculaService(repository, meterRegistry);
    }

    private static Pelicula pelicula(Integer id) {
        return new Pelicula(id, "Gladiador", "Accion", 2000, "Russell Crowe");
    }

    private double operaciones(String operacion) {
        Counter contador = meterRegistry.find("peliculas.operaciones").tag("operacion", operacion).counter();
        return contador == null ? 0 : contador.count();
    }

    @Test
    void listarDevuelveTodasLasPeliculasDelRepositorio() {
        when(repository.findAll()).thenReturn(List.of(pelicula(1), pelicula(2)));

        assertThat(service.listar()).hasSize(2);
    }

    @Test
    void buscarPorIdDevuelveLaPeliculaCuandoExiste() {
        when(repository.findById(1)).thenReturn(Optional.of(pelicula(1)));

        assertThat(service.buscarPorId(1)).contains(pelicula(1));
    }

    @Test
    void buscarPorIdDevuelveVacioCuandoNoExiste() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThat(service.buscarPorId(99)).isEmpty();
    }

    @Test
    void buscarPorTituloDelegaEnElRepositorioSinDistinguirMayusculas() {
        when(repository.findByTituloIgnoreCase("gladiador")).thenReturn(Optional.of(pelicula(1)));

        assertThat(service.buscarPorTitulo("gladiador")).isPresent();
    }

    @Test
    void guardarPeliculaPersisteYRegistraMetricaCrear() {
        when(repository.save(any(Pelicula.class))).thenReturn(pelicula(10));

        Pelicula guardada = service.guardarPelicula(pelicula(null));

        assertThat(guardada.getId()).isEqualTo(10);
        assertThat(operaciones("crear")).isEqualTo(1.0);
    }

    @Test
    void actualizarPeliculaExistenteAsignaElIdYRegistraMetrica() {
        when(repository.existsById(5)).thenReturn(true);
        when(repository.save(any(Pelicula.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Pelicula actualizada = service.actualizarPelicula(5, pelicula(null));

        assertThat(actualizada.getId()).isEqualTo(5);
        verify(repository).save(argThat(p -> p.getId() == 5));
        assertThat(operaciones("actualizar")).isEqualTo(1.0);
    }

    @Test
    void actualizarPeliculaInexistenteLanzaExcepcionYNoGuarda() {
        when(repository.existsById(99)).thenReturn(false);
        Pelicula datos = pelicula(null);

        assertThatThrownBy(() -> service.actualizarPelicula(99, datos))
                .isInstanceOf(PeliculaNoEncontradaException.class)
                .hasMessageContaining("99");
        verify(repository, never()).save(any());
        assertThat(operaciones("actualizar")).isZero();
    }

    @Test
    void eliminarPeliculaExistenteLaBorraYRegistraMetrica() {
        when(repository.existsById(3)).thenReturn(true);

        service.eliminarPorId(3);

        verify(repository).deleteById(3);
        assertThat(operaciones("eliminar")).isEqualTo(1.0);
    }

    @Test
    void eliminarPeliculaInexistenteLanzaExcepcionYNoBorra() {
        when(repository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminarPorId(99))
                .isInstanceOf(PeliculaNoEncontradaException.class);
        verify(repository, never()).deleteById(any());
    }
}
