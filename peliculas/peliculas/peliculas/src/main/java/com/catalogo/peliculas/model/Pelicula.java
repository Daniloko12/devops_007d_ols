package com.catalogo.peliculas.model;

import jakarta.persistence.*;                    // Permite mapear la clase a una tabla en la base de datos (JPA)
import lombok.AllArgsConstructor;               // Genera automáticamente un constructor con todos los atributos
import lombok.Data;                            // Genera getters, setters, toString, equals y hashCode automáticamente  
import lombok.NoArgsConstructor;              // Genera un constructor vacío (sin parámetros)
import jakarta.validation.constraints.*;     // Permite validar los datos (ej: campos obligatorios, valores mínimos, etc.)



@Entity                     // Indica que esta clase es una entidad que se mapeará a una tabla en la BD
@Table(name="pelicula",     // Define el nombre de la tabla en la base de datos
        // El título es único: la API busca por título y espera un solo resultado.
        // Además impide que dos réplicas carguen los mismos datos iniciales (ver DataLoader).
        uniqueConstraints = @UniqueConstraint(name = "uk_pelicula_titulo", columnNames = "titulo"))
@Data                       // Genera automáticamente getters, setters, toString, equals y hashCode
@NoArgsConstructor          // Genera un constructor vacío (necesario para JPA)
@AllArgsConstructor         // Genera un constructor con todos los atributos




public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

   @NotBlank(message = "El título no puede estar vacío")
   @Column(nullable = false, length = 100)
   private String titulo;

   @NotBlank(message = "El género no puede estar vacío")   
   private String genero;

   // Corregido: el valor (1999) no coincidía con el mensaje (1900)
   @Min(value = 1900, message = "El año debe ser mayor o igual a 1900")
   @NotNull(message = "El anio no puede ser nulo ")
   private Integer anio;

   @NotBlank(message = "El actor principal no puede estar vacío")
   private String actorPrincipal;

}

