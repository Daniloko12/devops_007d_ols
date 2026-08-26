package com.catalogo.peliculas.service;

import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.repository.PeliculaRepository;
import org.springframework.beans.factory.annotation.Autowired;// Permite la inyección de dependencias (Spring crea el objeto automáticamente)
import org.springframework.stereotype.Service;// Indica que la clase es un componente de tipo servicio dentro de Spring

import java.util.List;
import java.util.Optional;

@Service

public class PeliculaService {

    @Autowired // Inyección de dependencias: Spring crea el objeto automáticamente
    private PeliculaRepository repository;

    // GET: listar todas las películas ─────────────────────────────
    // Se llama al método findAll() del repository,
    // el cual ya viene implementado por JpaRepository
    // y retorna todas las películas de la base de dato
    public List<Pelicula> listar() {
        return repository.findAll();
    }

      // ── GET: buscar por título     ─────────────────────────────
      // Retorna UNA película por título (Optional porque puede existir o no)
    public Optional<Pelicula> buscarPorTitulo(String titulo) {
        return repository.findByTituloIgnoreCase(titulo);
    }

    // ── GET: buscar por ID ─────────────────────────────────
    // Retorna UNA película por ID (Optional porque puede no encontrarse)
    public Optional<Pelicula> buscarPorId(Integer id) {
        return repository.findById(id);
    }


     // ──  POST: agregar pelicula ─────────────────────────────────
    public Pelicula guardarPelicula(Pelicula pelicula) {
        return repository.save(pelicula);
    }


    // ── DELETE: eliminar por ID ─────────────────────────────────
    public void eliminarPorId(Integer id) {
    repository.deleteById(id);
    }

    // ── PUT: actualizar pelicula ─────────────────────────────────
    public Pelicula actualizarPelicula(Integer id, Pelicula pelicula) {
    pelicula.setId(id); 
    return repository.save(pelicula);
}

}