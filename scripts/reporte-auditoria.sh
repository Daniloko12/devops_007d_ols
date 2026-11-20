#!/usr/bin/env bash
# Genera el reporte de auditoría (Markdown) de una ejecución del pipeline:
# qué controles se ejecutaron, con qué umbral, su resultado y la decisión final.
# El pipeline lo publica en el resumen de la ejecución y como artefacto (90 días).
#
# Variables (las entrega el job reporte-auditoria del pipeline):
#   R_TESTS R_SONAR R_SNYK R_AUDITORIA R_DOCKER R_DEPLOY  -> success | failure | skipped | cancelled
#   VULNS TAG EVENTO RAMA ACTOR COMMIT RUN_URL RUN_NUMERO
set -euo pipefail

estado() {
  case "${1:-}" in
    success)   echo "✅ Aprobado" ;;
    failure)   echo "❌ Falló (bloquea)" ;;
    cancelled) echo "⚪ Cancelado" ;;
    skipped)   echo "⏭️ No ejecutado" ;;
    *)         echo "— ${1:-sin dato}" ;;
  esac
}

GATES_OK=true
for r in "${R_TESTS:-}" "${R_SONAR:-}" "${R_SNYK:-}" "${R_AUDITORIA:-}" "${R_DOCKER:-}"; do
  [[ "$r" == "success" ]] || GATES_OK=false
done

cat <<EOF
# Reporte de auditoría · ejecución #${RUN_NUMERO:-?}

| Campo | Valor |
|---|---|
| Fecha (UTC) | $(date -u +"%Y-%m-%d %H:%M:%S") |
| Commit | \`${COMMIT:-?}\` |
| Rama / evento | ${RAMA:-?} / ${EVENTO:-?} |
| Ejecutado por | ${ACTOR:-?} |
| Imagen | \`ghcr.io/daniloko12/devops_007d_ols:${TAG:-no generada}\` |
| Ejecución | ${RUN_URL:-?} |

## Controles automáticos

| # | Control | Herramienta | Política (umbral) | Referencia | Resultado |
|---|---|---|---|---|---|
| 1 | Pruebas automatizadas | Maven Surefire | 0 tests fallidos | ISO/IEC 27001:2022 A.8.29 | $(estado "${R_TESTS:-}") |
| 2 | Cobertura de código | JaCoCo | líneas y ramas ≥ 80 % | ISO/IEC 27001:2022 A.8.29 | $(estado "${R_TESTS:-}") |
| 3 | Análisis estático (SAST) | SonarCloud | Quality Gate aprobado | ISO/IEC 27001:2022 A.8.28 | $(estado "${R_SONAR:-}") |
| 4 | Dependencias vulnerables (SCA) | Snyk | 0 high/critical (encontradas: ${VULNS:-s/d}) | ISO/IEC 27001:2022 A.8.8 | $(estado "${R_SNYK:-}") |
| 5 | Secretos en el código | Gitleaks | 0 hallazgos | ISO/IEC 27001:2022 A.5.17 | $(estado "${R_AUDITORIA:-}") |
| 6 | Configuración insegura (Dockerfile, Kubernetes) | Trivy config | 0 HIGH/CRITICAL | CIS Docker / Kubernetes Benchmark, ISO A.8.9 | $(estado "${R_AUDITORIA:-}") |
| 7 | Vulnerabilidades de la imagen | Trivy image | 0 HIGH/CRITICAL con corrección | ISO/IEC 27001:2022 A.8.8 | $(estado "${R_DOCKER:-}") |
| 8 | Despliegue verificado y rollback | kind + smoke-test.sh | rollout y verificaciones OK | ISO/IEC 27001:2022 A.8.32 | $(estado "${R_DEPLOY:-}") |

Cambios a \`main\`: solo por Pull Request con todos los controles en verde (regla de protección de rama).

## Decisión
EOF

if [[ "$GATES_OK" == "true" ]]; then
  echo "**Todos los controles de calidad y seguridad se cumplieron.**"
  case "${R_DEPLOY:-}" in
    success) echo "La versión se desplegó, pasó las verificaciones y fue promovida." ;;
    failure) echo "El despliegue falló las verificaciones: se ejecutó el rollback y la versión no fue promovida." ;;
    *)       echo "Esta ejecución no despliega (solo \`main\` o ejecución manual despliegan)." ;;
  esac
else
  echo "**BLOQUEADO:** al menos un control falló. El pipeline terminó con error y no se publicó ni desplegó la imagen."
fi
