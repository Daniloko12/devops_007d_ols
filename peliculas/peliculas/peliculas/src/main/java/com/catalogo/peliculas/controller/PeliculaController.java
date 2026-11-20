package com.catalogo.peliculas.controller;

import com.catalogo.peliculas.dto.PeliculaRequest;
import com.catalogo.peliculas.exception.PeliculaNoEncontradaException;
import com.catalogo.peliculas.model.Pelicula;
import com.catalogo.peliculas.service.PeliculaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST del catálogo.
 * Cada endpoint responde con el código HTTP real (200, 201, 204, 400, 404, 500);
 * los errores los construye GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/peliculas")
@Tag(name = "Películas", description = "Operaciones sobre el catálogo de películas")
public class PeliculaController {

    private final PeliculaService service;

    public PeliculaController(PeliculaService service) {
        this.service = service;
    }

    // GET /peliculas
    @Operation(summary = "Listar películas", description = "Obtiene todas las películas registradas")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<Pelicula> listar() {
        return service.listar();
    }

    // GET /peliculas/titulo/{titulo}
    @Operation(summary = "Buscar película por título")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Película encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe una película con ese título")
    })
    @GetMapping("/titulo/{titulo}")
    public Pelicula buscarPorTitulo(@PathVariable String titulo) {
        return service.buscarPorTitulo(titulo)
                .orElseThrow(PeliculaNoEncontradaException::porTitulo);
    }

    // GET /peliculas/{id}   (antes quedaba publicado como /peliculas/peliculas/{id})
    @Operation(summary = "Buscar película por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Película encontrada"),
            @ApiResponse(responseCode = "400", description = "El id no es numérico"),
            @ApiResponse(responseCode = "404", description = "No existe una película con ese id")
    })
    @GetMapping("/{id}")
    public Pelicula buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id)
                .orElseThrow(() -> PeliculaNoEncontradaException.porId(id));
    }

    // POST /peliculas/agregar
    @Operation(summary = "Agregar película", description = "Registra una película nueva")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Película creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping("/agregar")
    public ResponseEntity<Pelicula> crearPelicula(@Valid @RequestBody PeliculaRequest request) {
        Pelicula creada = service.guardarPelicula(request.toEntity());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/peliculas/{id}")
                .buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    // PUT /peliculas/actualizar/{id}
    @Operation(summary = "Actualizar película", description = "Reemplaza los datos de una película existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Película actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "No existe una película con ese id")
    })
    @PutMapping("/actualizar/{id}")
    public Pelicula actualizar(@PathVariable Integer id, @Valid @RequestBody PeliculaRequest request) {
        return service.actualizarPelicula(id, request.toEntity());
    }

    // DELETE /peliculas/eliminar/{id}
    @Operation(summary = "Eliminar película")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Película eliminada"),
            @ApiResponse(responseCode = "404", description = "No existe una película con ese id")
    })
    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
