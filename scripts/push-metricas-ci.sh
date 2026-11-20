#!/usr/bin/env bash
# Genera las métricas del pipeline desde los reportes de Maven y las envía al
# Pushgateway del clúster, donde Prometheus las lee y Grafana las muestra en
# la fila "CI/CD" del dashboard.
#
# Uso local (después de ./mvnw clean verify):   ./scripts/push-metricas-ci.sh
# En el pipeline se definen además CI_BUILD_SEGUNDOS, CI_DESPLIEGUE_SEGUNDOS, etc.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROYECTO="$ROOT/peliculas/peliculas/peliculas"
ARCHIVO="${ARCHIVO_METRICAS:-$PROYECTO/target/pipeline-metrics.prom}"

python3 "$ROOT/scripts/metricas-ci.py" --proyecto "$PROYECTO" --salida "$ARCHIVO"

# Si no se indica la URL, se abre un port-forward temporal al Pushgateway del clúster
if [[ -z "${PUSHGATEWAY_URL:-}" ]]; then
  kubectl -n monitoring port-forward svc/pushgateway 19091:9091 >/dev/null 2>&1 &
  PF_PID=$!
  trap 'kill "$PF_PID" 2>/dev/null || true' EXIT
  PUSHGATEWAY_URL="http://localhost:19091"
  for _ in $(seq 1 20); do
    curl -s -o /dev/null "$PUSHGATEWAY_URL" && break
    sleep 1
  done
fi

# PUT reemplaza el grupo completo "job=pipeline": siempre queda la última ejecución
curl --fail --silent --show-error -X PUT --data-binary "@$ARCHIVO" \
  "$PUSHGATEWAY_URL/metrics/job/pipeline"

echo "Métricas enviadas a $PUSHGATEWAY_URL (grupo job=pipeline)"
