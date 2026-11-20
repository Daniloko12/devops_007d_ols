package com.catalogo.peliculas.exception;

/**
 * Se lanza cuando se busca, actualiza o elimina una película que no existe.
 * GlobalExceptionHandler la transforma en una respuesta HTTP 404.
 */
public class PeliculaNoEncontradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private PeliculaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static PeliculaNoEncontradaException porId(Integer id) {
        return new PeliculaNoEncontradaException("No existe una película con id " + id);
    }

    public static PeliculaNoEncontradaException porTitulo() {
        // No se incluye el título recibido para no reflejar datos del cliente en logs/respuestas
        return new PeliculaNoEncontradaException("No existe una película con el título indicado");
    }
}
