package com.catalogo.peliculas.demo;

/**
 * DEMOSTRACIÓN del gate de cobertura: lógica con muchas ramas y sin ninguna prueba.
 * Baja la cobertura por debajo del 80 % y JaCoCo debe bloquear el cambio. No se integra.
 */
public final class CalculadoraTarifa {

    private CalculadoraTarifa() {
    }

    public static long precio(String formato, int edad, boolean estreno, int diaSemana) {
        double base = switch (formato) {
            case "2D" -> 4500;
            case "3D" -> 6500;
            case "IMAX" -> 8900;
            case "4DX" -> 9900;
            default -> 5000;
        };
        if (edad < 12) {
            base *= 0.6;
        } else if (edad >= 65) {
            base *= 0.5;
        } else if (edad < 25) {
            base *= 0.8;
        }
        if (estreno) {
            base += 1500;
        }
        if (diaSemana == 2 || diaSemana == 3) {
            base *= 0.7;
        }
        if (base > 10000) {
            base = 10000;
        }
        return Math.round(base);
    }

    public static String categoria(int duracionMinutos, int anio) {
        if (duracionMinutos <= 0 || anio < 1900) {
            return "INVALIDA";
        }
        if (duracionMinutos < 40) {
            return "CORTOMETRAJE";
        }
        if (anio < 1960) {
            return duracionMinutos > 150 ? "CLASICO_EPICO" : "CLASICO";
        }
        if (anio < 2000) {
            return duracionMinutos > 150 ? "EPICO" : "CATALOGO";
        }
        return duracionMinutos > 150 ? "ESTRENO_EPICO" : "ESTRENO";
    }
}
