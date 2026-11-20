#!/usr/bin/env bash
# Crea el Secret "mysql-credentials" en el namespace peliculas.
# - En el pipeline las claves llegan como variables de entorno (GitHub Secrets).
# - En local, si no hay variables, se generan claves aleatorias.
# - Si el Secret ya existe NO se modifica: cambiar las claves de una BD ya
#   inicializada dejaría a la app y al exporter sin acceso.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NS=peliculas

# Namespace con sus etiquetas de seguridad (Pod Security "restricted") desde el inicio
kubectl apply -f "$ROOT/k8s/namespace.yaml" >/dev/null

if kubectl -n "$NS" get secret mysql-credentials >/dev/null 2>&1; then
  echo "Secret mysql-credentials ya existe: se mantiene sin cambios."
  exit 0
fi

genera_clave() { openssl rand -hex 16; }

kubectl -n "$NS" create secret generic mysql-credentials \
  --from-literal=root-password="${MYSQL_ROOT_PASSWORD:-$(genera_clave)}" \
  --from-literal=app-password="${MYSQL_APP_PASSWORD:-$(genera_clave)}" \
  --from-literal=exporter-password="${MYSQL_EXPORTER_PASSWORD:-$(genera_clave)}" \
  >/dev/null

echo "Secret mysql-credentials creado."
