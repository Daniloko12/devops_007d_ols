package com.catalogo.peliculas;

import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.repository.PeliculaRepository;
import org.springframework.boot.CommandLineRunner; // Permite ejecutar código automáticamente al iniciar la aplicación
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Permiten definir configuraciones y beans en Spring

@Configuration
public class DataLoader {

    @Bean // indica que se ejecutara al iniciar la aplicacion
    CommandLineRunner init(PeliculaRepository repository) {
          // Se inyecta el repository automáticamente
     return args -> {
        // Verifica si la tabla está vacía
         if (repository.count() == 0) {

          repository.save(new Pelicula(null, "Gladiador", "Acción", 2000, "Russell Crowe"));
          repository.save(new Pelicula(null, "El Señor de los Anillos", "Fantasía", 2001, "Elijah Wood"));
          repository.save(new Pelicula(null, "Batman: El Caballero de la Noche", "Acción", 2008, "Christian Bale"));
          repository.save(new Pelicula(null, "Avengers", "Acción", 2012, "Robert Downey Jr"));
          repository.save(new Pelicula(null, "Interstellar", "Ciencia Ficción", 2014, "Matthew McConaughey"));
          repository.save(new Pelicula(null, "Parásitos", "Drama", 2019, "Song Kang-ho"));

            }

        };
    }
}
