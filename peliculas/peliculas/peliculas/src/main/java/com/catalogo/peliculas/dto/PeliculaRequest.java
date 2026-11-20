package com.catalogo.peliculas.dto;

import com.catalogo.peliculas.model.Pelicula;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos que el cliente envía para crear o actualizar una película.
 *
 * Se usa este DTO en lugar de la entidad JPA en los @RequestBody para evitar
 * "mass assignment": el cliente no puede fijar campos internos como el id.
 * (Regla de SonarCloud java:S4684 - "Persistent entities should not be used
 * as arguments of @RequestMapping methods").
 */
public record PeliculaRequest(

        @NotBlank(message = "El título no puede estar vacío")
        String titulo,

        @NotBlank(message = "El género no puede estar vacío")
        String genero,

        @NotNull(message = "El año no puede ser nulo")
        @Min(value = 1900, message = "El año debe ser mayor o igual a 1900")
        Integer anio,

        @NotBlank(message = "El actor principal no puede estar vacío")
        String actorPrincipal) {

    /** Convierte la petición en una entidad nueva (sin id: lo asigna la base de datos). */
    public Pelicula toEntity() {
        return new Pelicula(null, titulo, genero, anio, actorPrincipal);
    }
}
