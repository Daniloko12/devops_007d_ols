#!/usr/bin/env bash
# Auditoría automática de configuración: verifica que una rama tenga las reglas de
# protección que exige la política del proyecto (.github/rulesets/proteccion-ramas.json).
# Imprime "ok" o la lista de lo que falta. La usa el job reporte-auditoria del pipeline
# y scripts/configurar-repositorio.sh.
#
# Uso:  ./scripts/auditar-proteccion-rama.sh <owner/repo> [rama]
# Requiere gh autenticado (en el pipeline: GH_TOKEN = github.token, solo lectura).
set -euo pipefail

REPO="$1"
RAMA="${2:-main}"
REGLAS_EXIGIDAS="pull_request required_status_checks non_fast_forward deletion"
CHECKS_EXIGIDOS="gate-tests-cobertura gate-sonarcloud gate-snyk gate-auditoria imagen-docker"

# Reglas efectivas de la rama (incluye las de todos los rulesets que la afectan)
if ! REGLAS_JSON="$(gh api "repos/$REPO/rules/branches/$RAMA" 2>/dev/null)"; then
  echo "sin datos (no se pudo consultar la API de GitHub)"
  exit 0
fi

REGLAS_JSON="$REGLAS_JSON" python3 - "$REGLAS_EXIGIDAS" "$CHECKS_EXIGIDOS" <<'EOF'
import json, os, sys

try:
    reglas = json.loads(os.environ.get("REGLAS_JSON") or "[]")
except json.JSONDecodeError:
    reglas = []
if not isinstance(reglas, list):
    reglas = []

tipos = {r.get("type") for r in reglas}
contextos = {
    c.get("context")
    for r in reglas if r.get("type") == "required_status_checks"
    for c in r.get("parameters", {}).get("required_status_checks", [])
}
faltan = [t for t in sys.argv[1].split() if t not in tipos]
faltan += [f"check {c}" for c in sys.argv[2].split() if c not in contextos]
print("ok" if not faltan else "faltan: " + ", ".join(faltan))
EOF
