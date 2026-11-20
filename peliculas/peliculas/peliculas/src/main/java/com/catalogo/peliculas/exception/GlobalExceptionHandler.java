package com.catalogo.peliculas.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas HTTP con el código correcto y cuerpo
 * estándar RFC 9457 (ProblemDetail, content-type application/problem+json).
 *
 * Importancia para observabilidad: con códigos reales (404, 400, 500) la métrica
 * http_server_requests_seconds_count{status="..."} permite graficar los errores
 * en Grafana, y los log.warn / log.error quedan disponibles en Loki.
 *
 * Al extender ResponseEntityExceptionHandler, los errores estándar de Spring MVC
 * (JSON mal formado, tipo de parámetro inválido, ruta inexistente, método no
 * permitido, etc.) también responden con su código correcto en vez de 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404: la película solicitada no existe. */
    @ExceptionHandler(PeliculaNoEncontradaException.class)
    public ProblemDetail manejarPeliculaNoEncontrada(PeliculaNoEncontradaException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Película no encontrada");
        return problema;
    }

    /** 400: el cuerpo de la petición no cumple las validaciones (@NotBlank, @Min, ...). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        log.warn("Solicitud rechazada por validación en los campos {}", errores.keySet());

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Uno o más campos no son válidos");
        problema.setTitle("Datos inválidos");
        problema.setProperty("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problema);
    }

    /**
     * 500: cualquier error no previsto. Se registra completo en el log (nivel ERROR,
     * con traceId) pero al cliente no se le exponen detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado al procesar la solicitud", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Revise los logs usando el traceId de la solicitud.");
        problema.setTitle("Error interno del servidor");
        return problema;
    }
}
