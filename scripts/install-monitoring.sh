#!/usr/bin/env bash
# Instala (o actualiza) el stack de observabilidad en el namespace "monitoring":
#   kube-prometheus-stack (Prometheus + Grafana), Loki, Alloy y Pushgateway.
# Es idempotente (se puede ejecutar varias veces). Lo usan local-up.sh y el pipeline.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NS=monitoring
VALUES="$ROOT/monitoring"

# shellcheck source=../monitoring/versions.env
source "$VALUES/versions.env"

# Devuelve "--version X" solo si la versión está fijada en versions.env
version_de() { [[ -n "${1:-}" ]] && echo "--version $1" || true; }

echo "==> Repositorios de Helm"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts --force-update >/dev/null
helm repo add grafana https://grafana.github.io/helm-charts --force-update >/dev/null
helm repo add grafana-community https://grafana-community.github.io/helm-charts --force-update >/dev/null
helm repo update >/dev/null

kubectl create namespace "$NS" --dry-run=client -o yaml | kubectl apply -f - >/dev/null

# Clave de Grafana: se reutiliza si ya existe; si no, se toma de GRAFANA_ADMIN_PASSWORD
# o se genera una aleatoria. Nunca queda escrita en el repositorio.
if kubectl -n "$NS" get secret kps-grafana >/dev/null 2>&1; then
  GRAFANA_ADMIN_PASSWORD="$(kubectl -n "$NS" get secret kps-grafana -o jsonpath='{.data.admin-password}' | base64 -d)"
else
  GRAFANA_ADMIN_PASSWORD="${GRAFANA_ADMIN_PASSWORD:-$(openssl rand -hex 12)}"
fi

echo "==> kube-prometheus-stack (Prometheus Operator, Prometheus, Grafana, kube-state-metrics, node-exporter)"
# shellcheck disable=SC2046
helm upgrade --install kps prometheus-community/kube-prometheus-stack \
  --namespace "$NS" \
  --values "$VALUES/kube-prometheus-stack-values.yaml" \
  --set-string grafana.adminPassword="$GRAFANA_ADMIN_PASSWORD" \
  $(version_de "$KPS_CHART_VERSION") \
  --wait --timeout 10m

echo "==> Loki (almacenamiento de logs)"
# shellcheck disable=SC2046
helm upgrade --install loki grafana-community/loki \
  --namespace "$NS" \
  --values "$VALUES/loki-values.yaml" \
  $(version_de "$LOKI_CHART_VERSION") \
  --wait --timeout 10m

echo "==> Alloy (recolector de logs de los pods)"
# shellcheck disable=SC2046
helm upgrade --install alloy grafana/alloy \
  --namespace "$NS" \
  --values "$VALUES/alloy-values.yaml" \
  $(version_de "$ALLOY_CHART_VERSION") \
  --wait --timeout 5m

echo "==> Pushgateway (métricas del pipeline CI/CD)"
# shellcheck disable=SC2046
helm upgrade --install pushgateway prometheus-community/prometheus-pushgateway \
  --namespace "$NS" \
  --values "$VALUES/pushgateway-values.yaml" \
  $(version_de "$PUSHGATEWAY_CHART_VERSION") \
  --wait --timeout 5m

echo "==> Versiones instaladas (para fijarlas en monitoring/versions.env)"
helm list --namespace "$NS"

# En el pipeline (CI=true) nunca se imprime la clave en los logs
if [[ -z "${CI:-}" ]]; then
  echo
  echo "Grafana -> usuario: admin | clave: $GRAFANA_ADMIN_PASSWORD"
fi
