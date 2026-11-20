package com.catalogo.peliculas.service;

import com.catalogo.peliculas.exception.PeliculaNoEncontradaException;
import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.repository.PeliculaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PeliculaService {

    private static final Logger log = LoggerFactory.getLogger(PeliculaService.class);

    // Métrica de negocio: en Prometheus es el contador peliculas_operaciones_total,
    // con la etiqueta "operacion" (crear, actualizar o eliminar)
    private static final String METRICA_OPERACIONES = "peliculas.operaciones";

    private final PeliculaRepository repository;
    private final MeterRegistry meterRegistry;

    // Inyección por constructor (recomendada por Spring y por SonarCloud en lugar de @Autowired en atributos)
    public PeliculaService(PeliculaRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    // ── GET: listar todas las películas ─────────────────────────────
    public List<Pelicula> listar() {
        return repository.findAll();
    }

    // ── GET: buscar por título (Optional porque puede no existir) ───
    public Optional<Pelicula> buscarPorTitulo(String titulo) {
        return repository.findByTituloIgnoreCase(titulo);
    }

    // ── GET: buscar por ID (Optional porque puede no existir) ───────
    public Optional<Pelicula> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    // ── POST: agregar película ──────────────────────────────────────
    public Pelicula guardarPelicula(Pelicula pelicula) {
        Pelicula guardada = repository.save(pelicula);
        registrarOperacion("crear");
        log.info("Película creada con id={}", guardada.getId());
        return guardada;
    }

    // ── PUT: actualizar película (404 si no existe) ─────────────────
    public Pelicula actualizarPelicula(Integer id, Pelicula pelicula) {
        if (!repository.existsById(id)) {
            throw PeliculaNoEncontradaException.porId(id);
        }
        pelicula.setId(id);
        Pelicula actualizada = repository.save(pelicula);
        registrarOperacion("actualizar");
        log.info("Película actualizada con id={}", id);
        return actualizada;
    }

    // ── DELETE: eliminar por ID (404 si no existe) ──────────────────
    public void eliminarPorId(Integer id) {
        if (!repository.existsById(id)) {
            throw PeliculaNoEncontradaException.porId(id);
        }
        repository.deleteById(id);
        registrarOperacion("eliminar");
        log.info("Película eliminada con id={}", id);
    }

    private void registrarOperacion(String operacion) {
        Counter.builder(METRICA_OPERACIONES)
                .description("Operaciones de escritura realizadas sobre el catálogo")
                .tag("operacion", operacion)
                .register(meterRegistry)
                .increment();
    }
}
