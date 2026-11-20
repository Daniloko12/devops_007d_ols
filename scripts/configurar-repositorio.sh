#!/usr/bin/env bash
# Aplica los controles de gobernanza del repositorio en GitHub (idempotente):
#   1. Ruleset de protección de main y develop (.github/rulesets/proteccion-ramas.json):
#      - cambios solo por Pull Request (merge commit), conversaciones resueltas,
#      - 5 gates del pipeline como checks obligatorios (emitidos por GitHub Actions),
#      - sin force-push ni borrado de la rama, sin excepciones para administradores.
#   2. Seguridad del repositorio: alertas de Dependabot, actualizaciones de seguridad
#      automáticas, secret scanning con push protection y reporte privado de vulnerabilidades.
#
# Requisitos: gh autenticado con permisos de administración del repositorio (gh auth login).
# Uso:  ./scripts/configurar-repositorio.sh [owner/repo]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="${1:-Daniloko12/devops_007d_ols}"
ARCHIVO="$ROOT/.github/rulesets/proteccion-ramas.json"

command -v gh >/dev/null || { echo "Falta 'gh' (GitHub CLI) en el PATH."; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Ejecuta primero: gh auth login"; exit 1; }

echo "==> 1/2 Ruleset de protección de ramas"
NOMBRE="$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["name"])' "$ARCHIVO")"
ID="$(gh api "repos/$REPO/rulesets" --jq ".[] | select(.name == \"$NOMBRE\") | .id")"
if [[ -n "$ID" ]]; then
  gh api --method PUT "repos/$REPO/rulesets/$ID" --input "$ARCHIVO" >/dev/null
  echo "Ruleset '$NOMBRE' actualizado (id $ID)."
else
  ID="$(gh api --method POST "repos/$REPO/rulesets" --input "$ARCHIVO" --jq '.id')"
  echo "Ruleset '$NOMBRE' creado (id $ID)."
fi

echo "==> 2/2 Seguridad del repositorio"
gh api --method PUT "repos/$REPO/vulnerability-alerts" >/dev/null
echo "Alertas de Dependabot: activadas."
gh api --method PUT "repos/$REPO/automated-security-fixes" >/dev/null
echo "Actualizaciones de seguridad de Dependabot: activadas."
gh api --method PATCH "repos/$REPO" --input - >/dev/null <<'JSON'
{
  "security_and_analysis": {
    "secret_scanning": { "status": "enabled" },
    "secret_scanning_push_protection": { "status": "enabled" }
  }
}
JSON
echo "Secret scanning y push protection: activados."
gh api --method PUT "repos/$REPO/private-vulnerability-reporting" >/dev/null
echo "Reporte privado de vulnerabilidades: activado."

echo
echo "==> Verificación (misma auditoría que ejecuta el pipeline)"
for rama in main develop; do
  echo "  $rama: $("$ROOT/scripts/auditar-proteccion-rama.sh" "$REPO" "$rama")"
done
echo "  Seguridad: $(gh api "repos/$REPO" --jq '.security_and_analysis | to_entries | map("\(.key)=\(.value.status)") | join(", ")')"
