package com.catalogo.peliculas.controller;

import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.service.PeliculaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;// Permite la inyección automática de dependencias
import org.springframework.web.bind.annotation.*;// Contiene las anotaciones para crear endpoints REST (GET, POST, etc.)
//import com.catalogo.peliculas.hoteas.PeliculaModelAssembler;
//import org.springframework.hateoas.EntityModel;


import java.util.List;
import java.util.Optional;

@RestController

@RequestMapping("/peliculas")
public class PeliculaController {

    @Autowired
    private PeliculaService service;
    // Se inyecta el servicio para acceder a la lógica de negocio

// GET: listar todas las películas
    @Operation(
        summary = "Listar películas",
        description = "Obtiene una lista con todas las películas registradas en el sistema"
)
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos")  ,
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")        
})
    @GetMapping
    public List<Pelicula> listar() {
        return service.listar();
    }

    // GET: buscar por título
    @GetMapping("/titulo/{titulo}")
    public Optional<Pelicula> buscarPorTitulo(@PathVariable String titulo) {
        return service.buscarPorTitulo(titulo);
    }

   // GET: buscar por ID
    @GetMapping("peliculas/{id}")
    public Optional<Pelicula> buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }
@Operation(
        summary = "Agergar Peliculas",
        description = "agregra peliculas nuevas"
)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pelicula Agregada"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos")  ,
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")        
})
    // POST: agregar pelicula
    @PostMapping("/agregar")
    public Pelicula crearPelicula(@RequestBody Pelicula pelicula) {
        return service.guardarPelicula(pelicula);
    }

    // DELETE: eliminar por ID
    @DeleteMapping("eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {

    Optional<Pelicula> pelicula = service.buscarPorId(id);

    if (pelicula.isPresent()) {
        service.eliminarPorId(id);
        return "Película eliminada correctamente";
    } else {
        return "Película no encontrada con id: " + id;
    }
    }
@Operation(
        summary = "Actualizar Peliculas",
        description = "Actualiza Peliculas ya Existentes"
)
    // PUT: actualizar por ID
@PutMapping("actualizar/{id}")
public String actualizar(@PathVariable Integer id, @RequestBody Pelicula pelicula) {

    Optional<Pelicula> existente = service.buscarPorId(id);

    if (existente.isPresent()) {
        service.actualizarPelicula(id, pelicula);
        return "Película actualizada correctamente";
    } else {
        return "Película no encontrada con id: " + id;
    }
}
// agrege este comentario para poder poder hacer mi segundo Pull Request en GitHub
}