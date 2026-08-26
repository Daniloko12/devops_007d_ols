package com.catalogo.peliculas.repository;

import com.catalogo.peliculas.model.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//import java.util.List;
import java.util.Optional;

@Repository // Indica que esta interfaz es un componente de acceso a datos

    // Se usa "interface" y no "class" porque Spring Data JPA
    // genera automáticamente la implementación en tiempo de ejecución.

    // Al extender JpaRepository, ya tenemos métodos listos como:
    // - findAll()        -> listar todas las películas
    // - findById(id)     -> buscar por ID
    // - save(pelicula)   -> insertar o actualizar
    // - deleteById(id)   -> eliminar por ID

public interface PeliculaRepository extends JpaRepository<Pelicula, Integer> {

    // Buscar por título (ignorando mayúsculas/minúsculas)
    Optional<Pelicula> findByTituloIgnoreCase(String titulo);
    
}
