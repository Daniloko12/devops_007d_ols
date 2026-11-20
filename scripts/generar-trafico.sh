#!/usr/bin/env bash
# Genera tráfico variado contra la API para poblar el dashboard:
# respuestas 200, 201, 204, 400 y 404, y operaciones de negocio (crear/eliminar).
#
# Uso:  ./scripts/generar-trafico.sh [segundos]      (por defecto 120)
#       BASE_URL=http://localhost:8080 ./scripts/generar-trafico.sh 60
set -euo pipefail

DURACION="${1:-120}"
declare -A CONTEO=()

if [[ -z "${BASE_URL:-}" ]]; then
  kubectl -n peliculas port-forward svc/peliculas 18081:8080 >/dev/null 2>&1 &
  PF_PID=$!
  trap 'kill "$PF_PID" 2>/dev/null || true' EXIT
  BASE_URL="http://localhost:18081"
  for _ in $(seq 1 20); do
    curl -s -o /dev/null "$BASE_URL/actuator/health" && break
    sleep 1
  done
fi

# pedir <método> <ruta> [cuerpo JSON]  -> registra el código HTTP obtenido
pedir() {
  local codigo
  if [[ -n "${3:-}" ]]; then
    codigo="$(curl -s -o /dev/null -w '%{http_code}' -X "$1" -H 'Content-Type: application/json' -d "$3" "$BASE_URL$2" || true)"
  else
    codigo="$(curl -s -o /dev/null -w '%{http_code}' -X "$1" "$BASE_URL$2" || true)"
  fi
  CONTEO[$codigo]=$(( ${CONTEO[$codigo]:-0} + 1 ))
}

# Crea una película y la elimina (mantiene el catálogo estable)
crear_y_eliminar() {
  local respuesta id
  respuesta="$(curl -s -X POST -H 'Content-Type: application/json' \
    -d '{"titulo":"Pelicula de prueba","genero":"Drama","anio":2024,"actorPrincipal":"Actor Demo"}' \
    -w '\n%{http_code}' "$BASE_URL/peliculas/agregar" || true)"
  CONTEO[${respuesta##*$'\n'}]=$(( ${CONTEO[${respuesta##*$'\n'}]:-0} + 1 ))
  id="$(grep -o '"id":[0-9]*' <<<"$respuesta" | head -1 | cut -d: -f2 || true)"
  if [[ -n "$id" ]]; then
    pedir DELETE "/peliculas/eliminar/$id"
  fi
}

echo "Generando tráfico durante ${DURACION}s contra $BASE_URL ..."
FIN=$((SECONDS + DURACION))
while ((SECONDS < FIN)); do
  pedir GET /peliculas                                   # 200
  pedir GET "/peliculas/$((RANDOM % 6 + 1))"             # 200
  pedir GET /peliculas/titulo/Gladiador                  # 200
  pedir GET /peliculas/titulo/Interstellar               # 200
  pedir GET /peliculas/999999                            # 404
  pedir GET /peliculas/abc                               # 400
  pedir POST /peliculas/agregar '{"titulo":"","genero":"Drama","anio":1800,"actorPrincipal":"X"}'  # 400
  crear_y_eliminar                                       # 201 + 204
  sleep 0.3
done

echo "Resumen por código HTTP:"
for codigo in $(printf '%s\n' "${!CONTEO[@]}" | sort); do
  printf '  HTTP %s: %d\n' "$codigo" "${CONTEO[$codigo]}"
done
