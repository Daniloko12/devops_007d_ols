package com.catalogo.peliculas.model;

import jakarta.persistence.*;                    // Permite mapear la clase a una tabla en la base de datos (JPA)
import lombok.AllArgsConstructor;               // Genera automáticamente un constructor con todos los atributos
import lombok.Data;                            // Genera getters, setters, toString, equals y hashCode automáticamente  
import lombok.NoArgsConstructor;              // Genera un constructor vacío (sin parámetros)
import jakarta.validation.constraints.*;     // Permite validar los datos (ej: campos obligatorios, valores mínimos, etc.)


//import java.util.Date;

@Entity                     // Indica que esta clase es una entidad que se mapeará a una tabla en la BD
@Table(name="pelicula")     // Define el nombre de la tabla en la base de datos
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

   @Min(value = 1999, message = "El año debe ser mayor a 1900")
   @NotNull(message = "El anio no puede ser nulo ")
   private Integer anio;

   @NotBlank(message = "El actor principal no puede estar vacío")
   private String actorPrincipal;

};

