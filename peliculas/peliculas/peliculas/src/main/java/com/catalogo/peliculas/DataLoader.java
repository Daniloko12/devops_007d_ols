package com.catalogo.peliculas;

import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.repository.PeliculaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner; // Permite ejecutar código automáticamente al iniciar la aplicación
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Carga el catálogo inicial al arrancar la aplicación.
 *
 * Con 2 réplicas en Kubernetes, las dos arrancan a la vez sobre la misma base de datos.
 * Por eso la carga es segura frente a ejecuciones simultáneas:
 *  1. Elimina títulos duplicados que hayan quedado de versiones anteriores
 *     (se conserva el registro más antiguo de cada título).
 *  2. Si la tabla está vacía, inserta cada película por separado y solo si su título
 *     no existe. Si otra réplica la insertó primero, la restricción única del título
 *     (uk_pelicula_titulo) rechaza el duplicado y se continúa con la siguiente.
 */
@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private static final String GENERO_ACCION = "Acción";

    @Bean // indica que se ejecutara al iniciar la aplicacion
    CommandLineRunner init(PeliculaRepository repository) {
        return args -> {
            eliminarDuplicados(repository);
            // Verifica si la tabla está vacía
            if (repository.count() == 0) {
                peliculasIniciales().forEach(pelicula -> cargarSiNoExiste(repository, pelicula));
            }
        };
    }

    static List<Pelicula> peliculasIniciales() {
        return List.of(
                new Pelicula(null, "Gladiador", GENERO_ACCION, 2000, "Russell Crowe"),
                new Pelicula(null, "El Señor de los Anillos", "Fantasía", 2001, "Elijah Wood"),
                new Pelicula(null, "Batman: El Caballero de la Noche", GENERO_ACCION, 2008, "Christian Bale"),
                new Pelicula(null, "Avengers", GENERO_ACCION, 2012, "Robert Downey Jr"),
                new Pelicula(null, "Interstellar", "Ciencia Ficción", 2014, "Matthew McConaughey"),
                new Pelicula(null, "Parásitos", "Drama", 2019, "Song Kang-ho"));
    }

    /** Deja un solo registro por título (sin distinguir mayúsculas), el de menor id. */
    static void eliminarDuplicados(PeliculaRepository repository) {
        List<Pelicula> sobrantes = repository.findAll(Sort.by("id")).stream()
                .collect(Collectors.groupingBy(p -> p.getTitulo().toLowerCase(Locale.ROOT)))
                .values().stream()
                .filter(mismoTitulo -> mismoTitulo.size() > 1)
                .map(mismoTitulo -> mismoTitulo.subList(1, mismoTitulo.size()))
                .flatMap(Collection::stream)
                .toList();
        if (!sobrantes.isEmpty()) {
            repository.deleteAll(sobrantes);
            log.warn("Se eliminaron {} películas con título duplicado", sobrantes.size());
        }
    }

    static void cargarSiNoExiste(PeliculaRepository repository, Pelicula pelicula) {
        if (repository.existsByTituloIgnoreCase(pelicula.getTitulo())) {
            return;
        }
        try {
            repository.save(pelicula);
        } catch (DataIntegrityViolationException e) {
            log.info("La película '{}' ya fue cargada por otra réplica", pelicula.getTitulo());
        }
    }
}
