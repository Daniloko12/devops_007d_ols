#!/usr/bin/env bash
# Pruebas de humo después de desplegar. Termina con exit code 1 si algo falla,
# por eso el pipeline puede usarlo como gate post-despliegue.
# Verifica que:
#   1. la app responde con los códigos HTTP esperados (200, 404, 400),
#   2. Prometheus está leyendo las métricas de la app, de MySQL y del Pushgateway,
#   3. Loki está recibiendo los logs de los pods.
set -euo pipefail

APP_PORT=18080
PROM_PORT=19090
LOKI_PORT=13100
FALLAS=0
PIDS=()

cerrar_port_forwards() {
  for pid in ${PIDS[@]+"${PIDS[@]}"}; do kill "$pid" 2>/dev/null || true; done
}
trap cerrar_port_forwards EXIT

ok()    { echo "  [OK]    $1"; }
falla() { echo "  [FALLA] $1"; FALLAS=$((FALLAS + 1)); }

# port_forward <namespace> <recurso> <puerto_local> <puerto_remoto>
port_forward() {
  kubectl -n "$1" port-forward "$2" "$3:$4" >/dev/null 2>&1 &
  PIDS+=("$!")
  for _ in $(seq 1 20); do
    curl -s -o /dev/null "http://localhost:$3" && return 0
    sleep 1
  done
  echo "No se pudo abrir el port-forward hacia $2"
  return 1
}

# verificar_http <descripción> <url> <código esperado>
verificar_http() {
  local codigo
  codigo="$(curl -s -o /dev/null -w '%{http_code}' "$2" || true)"
  if [[ "$codigo" == "$3" ]]; then ok "$1 -> HTTP $codigo"; else falla "$1 -> HTTP $codigo (esperado $3)"; fi
}

# Extrae el primer valor de una respuesta JSON de Prometheus/Loki ("" si no hay datos)
primer_valor() {
  python3 -c 'import sys, json
try:
    r = json.load(sys.stdin)["data"]["result"]
    print(r[0]["value"][1] if r else "")
except Exception:
    print("")'
}

# esperar_minimo <descripción> <comando que imprime un número> <mínimo> <segundos máximos>
esperar_minimo() {
  local descripcion=$1 comando=$2 minimo=$3 limite=$4 valor=""
  for ((t = 0; t < limite; t += 10)); do
    valor="$(eval "$comando" || true)"
    if [[ -n "$valor" ]] && python3 -c "import sys; sys.exit(0 if float('$valor') >= $minimo else 1)"; then
      ok "$descripcion (valor: $valor)"
      return 0
    fi
    sleep 10
  done
  falla "$descripcion (último valor: ${valor:-sin datos})"
}

consulta_prometheus() {
  curl -s --get "http://localhost:$PROM_PORT/api/v1/query" --data-urlencode "query=$1" | primer_valor
}

consulta_loki() {
  curl -s --get "http://localhost:$LOKI_PORT/loki/api/v1/query" --data-urlencode "query=$1" | primer_valor
}

echo "==> Esperando a que el despliegue esté completo"
kubectl -n peliculas rollout status statefulset/mysql --timeout=300s
kubectl -n peliculas rollout status deployment/peliculas --timeout=300s

echo "==> 1. Aplicación"
port_forward peliculas svc/peliculas "$APP_PORT" 8080
BASE="http://localhost:$APP_PORT"
verificar_http "Readiness (app + BD)" "$BASE/actuator/health/readiness" 200
verificar_http "Listar películas" "$BASE/peliculas" 200
# Búsqueda por título: falla con 500 si la carga inicial duplicó registros (2 réplicas a la vez)
verificar_http "Buscar por título" "$BASE/peliculas/titulo/Gladiador" 200
verificar_http "Película inexistente" "$BASE/peliculas/999999" 404
verificar_http "Id no numérico" "$BASE/peliculas/abc" 400
# Se guarda la respuesta completa antes de buscar: con "curl | grep -q" y pipefail,
# grep corta la lectura al encontrar la línea y curl termina con error (SIGPIPE).
METRICAS="$(curl -s "$BASE/actuator/prometheus" || true)"
if grep -q '^app_build_version' <<<"$METRICAS"; then
  ok "Métrica app_build_version publicada"
else
  falla "Métrica app_build_version no aparece en /actuator/prometheus"
fi

echo "==> 2. Prometheus (puede tardar hasta 3 min en descubrir los targets)"
port_forward monitoring svc/kps-prometheus "$PROM_PORT" 9090
# Todas las réplicas definidas en el Deployment deben estar siendo scrapeadas
REPLICAS="$(kubectl -n peliculas get deployment peliculas -o jsonpath='{.spec.replicas}')"
esperar_minimo "Réplicas de peliculas scrapeadas (up, esperadas: $REPLICAS)" \
  "consulta_prometheus 'sum(up{job=\"peliculas\"})'" "$REPLICAS" 180
esperar_minimo "MySQL disponible (mysql_up)" "consulta_prometheus 'max(mysql_up)'" 1 180
esperar_minimo "Pushgateway scrapeado (up)" "consulta_prometheus 'sum(up{job=\"pushgateway\"})'" 1 180

echo "==> 3. Loki"
port_forward monitoring svc/loki-gateway "$LOKI_PORT" 80
esperar_minimo "Líneas de log de peliculas en Loki (10 min)" \
  "consulta_loki 'sum(count_over_time({namespace=\"peliculas\"}[10m]))'" 1 120

echo
if ((FALLAS > 0)); then
  echo "RESULTADO: $FALLAS verificación(es) fallaron."
  exit 1
fi
echo "RESULTADO: todas las verificaciones pasaron."
