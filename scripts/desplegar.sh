#!/usr/bin/env bash
# Despliega el microservicio con una imagen específica y espera a que el rollout termine.
# Termina con exit code != 0 si el rollout no se completa (por ejemplo, pods que no arrancan).
#
# Uso:  ./scripts/desplegar.sh <repositorio-imagen> <tag>
#       ./scripts/desplegar.sh ghcr.io/daniloko12/devops_007d_ols sha-1a2b3c4
#
# SIMULAR_FALLA=true despliega esa versión con una URL de base de datos inexistente
# (solo para demostrar el rollback automático del pipeline).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ $# -eq 2 ]] || { echo "Uso: $0 <repositorio-imagen> <tag>"; exit 2; }
REPO_IMAGEN="$1"
TAG="$2"

# Se trabaja sobre una copia de k8s/ para no modificar los archivos del repositorio
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cp -r "$ROOT/k8s/." "$TMP/"

sed -i -e "s|newName: peliculas$|newName: ${REPO_IMAGEN}|" \
       -e "s|newTag: local$|newTag: ${TAG}|" "$TMP/kustomization.yaml"
grep -q "newTag: ${TAG}$" "$TMP/kustomization.yaml" || { echo "No se pudo fijar la imagen en kustomization.yaml"; exit 1; }

if [[ "${SIMULAR_FALLA:-false}" == "true" ]]; then
  echo "SIMULACIÓN DE FALLA: esta versión apunta a una base de datos inexistente"
  sed -i "s|jdbc:mysql://mysql:3306|jdbc:mysql://bd-inexistente:3306|" "$TMP/kustomization.yaml"
fi

echo "==> Desplegando ${REPO_IMAGEN}:${TAG}"
kubectl apply -k "$TMP"
# Queda registrado en "kubectl rollout history" qué imagen trae cada revisión
kubectl -n peliculas annotate deployment/peliculas \
  kubernetes.io/change-cause="${REPO_IMAGEN}:${TAG}" --overwrite >/dev/null

kubectl -n peliculas rollout status statefulset/mysql --timeout=300s
# progressDeadlineSeconds (k8s/deployment.yaml) corta antes si los pods no progresan
kubectl -n peliculas rollout status deployment/peliculas --timeout="${ROLLOUT_TIMEOUT:-300s}"
echo "==> Rollout completo: ${REPO_IMAGEN}:${TAG}"
