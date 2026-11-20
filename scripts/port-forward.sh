#!/usr/bin/env bash
# Abre accesos locales a la app, Grafana y Prometheus del clúster kind.
# Se mantiene abierto hasta que presiones Ctrl+C.
set -euo pipefail

PIDS=()
cerrar() { kill ${PIDS[@]+"${PIDS[@]}"} 2>/dev/null || true; }
trap cerrar EXIT INT TERM

kubectl -n peliculas port-forward svc/peliculas 8080:8080 >/dev/null 2>&1 &
PIDS+=("$!")
kubectl -n monitoring port-forward svc/kps-grafana 3000:80 >/dev/null 2>&1 &
PIDS+=("$!")
kubectl -n monitoring port-forward svc/kps-prometheus 9090:9090 >/dev/null 2>&1 &
PIDS+=("$!")

CLAVE_GRAFANA="$(kubectl -n monitoring get secret kps-grafana -o jsonpath='{.data.admin-password}' | base64 -d)"

cat <<EOF

  App (Swagger):  http://localhost:8080/swagger-ui.html
  Grafana:        http://localhost:3000        usuario: admin   clave: $CLAVE_GRAFANA
  Prometheus:     http://localhost:9090/targets   (alertas: /alerts)

  Ctrl+C para cerrar los accesos.
EOF

wait
