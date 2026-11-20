#!/usr/bin/env bash
# Levanta el entorno completo en tu PC:
#   clúster kind -> stack de monitoreo -> imagen Docker -> secretos -> MySQL + app -> verificación
# Se puede volver a ejecutar: reutiliza lo que ya existe y redespliega la app.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER=ep3
IMAGEN=peliculas:local

for comando in docker kind kubectl helm openssl python3 curl; do
  command -v "$comando" >/dev/null || { echo "Falta '$comando' en el PATH."; exit 1; }
done

echo "==> 1/6 Clúster kind '$CLUSTER'"
if kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  echo "El clúster ya existe: se reutiliza."
else
  kind create cluster --name "$CLUSTER" --config "$ROOT/monitoring/kind-config.yaml" --wait 120s
fi
kubectl config use-context "kind-$CLUSTER" >/dev/null

echo "==> 2/6 Stack de monitoreo"
"$ROOT/scripts/install-monitoring.sh"

echo "==> 3/6 Imagen Docker del microservicio"
COMMIT="$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo local)"
docker build -t "$IMAGEN" --build-arg GIT_COMMIT="$COMMIT" \
  -f "$ROOT/Dockerfile" "$ROOT/peliculas/peliculas/peliculas"
kind load docker-image "$IMAGEN" --name "$CLUSTER"

echo "==> 4/6 Secretos de la base de datos"
"$ROOT/scripts/create-secrets.sh"

echo "==> 5/6 Despliegue (kubectl apply -k k8s/)"
kubectl apply -k "$ROOT/k8s"
kubectl -n peliculas rollout status statefulset/mysql --timeout=300s
# En local la imagen siempre se llama peliculas:local; el restart obliga a usar la recién cargada
kubectl -n peliculas rollout restart deployment/peliculas
kubectl -n peliculas rollout status deployment/peliculas --timeout=300s

echo "==> 6/6 Verificación"
"$ROOT/scripts/smoke-test.sh"

echo
echo "Entorno listo. Para abrir la app, Grafana y Prometheus ejecuta: ./scripts/port-forward.sh"
