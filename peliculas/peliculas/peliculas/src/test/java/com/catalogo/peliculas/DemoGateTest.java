package com.catalogo.peliculas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** DEMOSTRACIÓN del gate de pruebas: esta prueba falla a propósito. No se integra. */
class DemoGateTest {

    @Test
    void pruebaQueFallaAProposito() {
        assertEquals(200, 500, "Demostración: el pipeline debe bloquear este cambio");
    }
}
