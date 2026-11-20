#!/usr/bin/env python3
"""
Genera las métricas del pipeline CI/CD en formato Prometheus a partir de los
reportes que deja "mvn verify":
  - target/site/jacoco/jacoco.csv          -> cobertura de líneas y ramas
  - target/surefire-reports/TEST-*.xml     -> tests ejecutados, fallidos y duración

Datos opcionales por variable de entorno (los entrega el pipeline):
  CI_BUILD_SEGUNDOS, CI_DESPLIEGUE_SEGUNDOS, CI_VULNERABILIDADES_ALTAS,
  CI_COMMIT, CI_RAMA

Uso:
  python3 scripts/metricas-ci.py --proyecto peliculas/peliculas/peliculas --salida pipeline.prom
"""
import argparse
import csv
import glob
import os
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


def git(*args):
    try:
        return subprocess.check_output(["git", *args], text=True, stderr=subprocess.DEVNULL).strip()
    except (OSError, subprocess.CalledProcessError):
        return "desconocido"


def leer_cobertura(proyecto):
    ruta = os.path.join(proyecto, "target", "site", "jacoco", "jacoco.csv")
    if not os.path.exists(ruta):
        sys.exit(f"No existe {ruta}. Ejecuta primero: ./mvnw clean verify")
    totales = {"LINE_MISSED": 0, "LINE_COVERED": 0, "BRANCH_MISSED": 0, "BRANCH_COVERED": 0}
    with open(ruta, newline="", encoding="utf-8") as f:
        for fila in csv.DictReader(f):
            for clave in totales:
                totales[clave] += int(fila[clave])

    def ratio(cubiertas, perdidas):
        total = cubiertas + perdidas
        return cubiertas / total if total else 1.0

    return (ratio(totales["LINE_COVERED"], totales["LINE_MISSED"]),
            ratio(totales["BRANCH_COVERED"], totales["BRANCH_MISSED"]))


def leer_tests(proyecto):
    archivos = glob.glob(os.path.join(proyecto, "target", "surefire-reports", "TEST-*.xml"))
    if not archivos:
        sys.exit("No hay reportes de Surefire. Ejecuta primero: ./mvnw clean verify")
    ejecutados = fallidos = 0
    duracion = 0.0
    for archivo in archivos:
        suite = ET.parse(archivo).getroot()
        ejecutados += int(suite.get("tests", 0))
        fallidos += int(suite.get("failures", 0)) + int(suite.get("errors", 0))
        duracion += float(suite.get("time", 0))
    return ejecutados, fallidos, duracion


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--proyecto", required=True, help="Carpeta del proyecto Maven")
    parser.add_argument("--salida", required=True, help="Archivo .prom a generar")
    args = parser.parse_args()

    commit = os.environ.get("CI_COMMIT") or git("rev-parse", "--short", "HEAD")
    rama = os.environ.get("CI_RAMA") or git("rev-parse", "--abbrev-ref", "HEAD")
    etiquetas = f'commit="{commit[:7]}",rama="{rama}"'

    lineas, ramas = leer_cobertura(args.proyecto)
    ejecutados, fallidos, duracion_tests = leer_tests(args.proyecto)

    metricas = [
        ("ci_cobertura_lineas_ratio", "Cobertura de líneas medida por JaCoCo (0 a 1)", lineas),
        ("ci_cobertura_ramas_ratio", "Cobertura de ramas medida por JaCoCo (0 a 1)", ramas),
        ("ci_tests_ejecutados", "Cantidad de tests ejecutados", ejecutados),
        ("ci_tests_fallidos", "Cantidad de tests fallidos o con error", fallidos),
        ("ci_tests_duracion_segundos", "Duración total de los tests", round(duracion_tests, 3)),
        ("ci_ultima_ejecucion_timestamp_segundos", "Momento de la última ejecución del pipeline", int(time.time())),
    ]
    opcionales = [
        ("CI_BUILD_SEGUNDOS", "ci_build_duracion_segundos", "Duración del job de build y pruebas"),
        ("CI_DESPLIEGUE_SEGUNDOS", "ci_despliegue_duracion_segundos", "Duración del despliegue en Kubernetes"),
        ("CI_VULNERABILIDADES_ALTAS", "ci_vulnerabilidades_altas", "Vulnerabilidades high/critical reportadas por Snyk"),
    ]
    for variable, nombre, ayuda in opcionales:
        if os.environ.get(variable):
            metricas.append((nombre, ayuda, float(os.environ[variable])))

    with open(args.salida, "w", encoding="utf-8") as f:
        for nombre, ayuda, valor in metricas:
            f.write(f"# HELP {nombre} {ayuda}\n# TYPE {nombre} gauge\n{nombre}{{{etiquetas}}} {valor}\n")

    print(f"Métricas del pipeline -> {args.salida}")
    print(f"  cobertura líneas: {lineas:.1%} | ramas: {ramas:.1%} | tests: {ejecutados} ({fallidos} fallidos)")


if __name__ == "__main__":
    main()
