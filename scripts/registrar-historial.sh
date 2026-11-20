#!/usr/bin/env bash
# Agrega una fila con las métricas de esta ejecución al archivo historial.csv
# de la rama "metrics-history". GitHub muestra el CSV como tabla, lo que permite
# ver la evolución de cobertura, tests y tiempos entre ejecuciones del pipeline.
#
# Uso (en el pipeline): ./scripts/registrar-historial.sh <archivo pipeline-metrics.prom>
# Variables opcionales: CI_COMMIT, CI_RAMA, CI_BUILD_SEGUNDOS, CI_DESPLIEGUE_SEGUNDOS,
#                       CI_VULNERABILIDADES_ALTAS, GITHUB_RUN_ID
set -euo pipefail

PROM="${1:?Indica el archivo .prom generado por metricas-ci.py}"
RAMA_HISTORIAL=metrics-history
ENCABEZADO="fecha_utc,commit,rama,cobertura_lineas_pct,cobertura_ramas_pct,tests,tests_fallidos,build_s,despliegue_s,vulnerabilidades_altas,run_id"

# valor <métrica> -> número de la línea "métrica{...} número" del archivo .prom
valor() { awk -v m="$1" 'index($1, m "{") == 1 { print $2 }' "$PROM"; }
porcentaje() {
  local v
  v="$(valor "$1")"
  if [[ -n "$v" ]]; then awk -v v="$v" 'BEGIN { printf "%.1f", v * 100 }'; fi
}

COMMIT="${CI_COMMIT:-$(git rev-parse HEAD)}"
FILA="$(date -u +%Y-%m-%dT%H:%M:%SZ),${COMMIT:0:7},${CI_RAMA:-$(git rev-parse --abbrev-ref HEAD)}"
FILA+=",$(porcentaje ci_cobertura_lineas_ratio),$(porcentaje ci_cobertura_ramas_ratio)"
FILA+=",$(valor ci_tests_ejecutados),$(valor ci_tests_fallidos)"
FILA+=",${CI_BUILD_SEGUNDOS:-},${CI_DESPLIEGUE_SEGUNDOS:-},${CI_VULNERABILIDADES_ALTAS:-},${GITHUB_RUN_ID:-local}"

DIR="$(mktemp -d)"
trap 'git worktree remove --force "$DIR" >/dev/null 2>&1 || true' EXIT

if git ls-remote --exit-code --heads origin "$RAMA_HISTORIAL" >/dev/null 2>&1; then
  git fetch --quiet --depth 1 origin "$RAMA_HISTORIAL"
  git worktree add --quiet --detach "$DIR" FETCH_HEAD
else
  echo "La rama $RAMA_HISTORIAL no existe: se crea vacía"
  git worktree add --quiet --detach "$DIR" HEAD
  git -C "$DIR" checkout --quiet --orphan "$RAMA_HISTORIAL"
  git -C "$DIR" rm -rf --quiet --ignore-unmatch .
fi

cd "$DIR"
[[ -f historial.csv ]] || echo "$ENCABEZADO" > historial.csv
echo "$FILA" >> historial.csv
git add historial.csv
git -c user.name="github-actions[bot]" \
    -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
    commit --quiet -m "historial: ejecución ${GITHUB_RUN_ID:-local} (${COMMIT:0:7})"
git push --quiet origin "HEAD:refs/heads/${RAMA_HISTORIAL}"

echo "Fila agregada a ${RAMA_HISTORIAL}/historial.csv:"
echo "  $FILA"
