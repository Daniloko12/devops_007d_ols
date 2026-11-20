# Política de seguridad

## Versiones con soporte

| Versión | Soporte |
|---|---|
| Rama `main` (imagen `ghcr.io/daniloko12/devops_007d_ols:stable`) | ✅ |
| Imágenes `sha-<commit>` anteriores a la `stable` vigente | ❌ |

## Cómo reportar una vulnerabilidad

No abras un issue público. Usa el reporte privado de GitHub:
pestaña **Security** del repositorio → **Report a vulnerability**.

Incluye la versión (commit o tag de imagen), los pasos para reproducir y el impacto observado.

## Controles automáticos

Cada cambio pasa por el pipeline `CI/CD Microservicio Peliculas` (`.github/workflows/ci.yml`).
Un control fallido termina el pipeline con código de salida distinto de 0 y bloquea la integración
y el despliegue. El detalle de cada control está en [docs/cumplimiento.md](docs/cumplimiento.md).

| Riesgo | Control | Herramienta |
|---|---|---|
| Código inseguro | Análisis estático y Quality Gate | SonarCloud |
| Dependencias vulnerables | 0 high/critical | Snyk |
| Secretos en el código | Escaneo del historial del cambio | Gitleaks, secret scanning + push protection de GitHub |
| Configuración insegura | Dockerfile y manifiestos de Kubernetes | Trivy config, Pod Security Standards `restricted` |
| Imagen vulnerable | 0 HIGH/CRITICAL con corrección | Trivy image |
| Dependencias desactualizadas | PR semanales y de seguridad | Dependabot |
| Cambios no revisados | PR obligatorio con gates requeridos, sin force-push | Ruleset `proteccion-main-develop` |

## Excepciones

Solo se aceptan excepciones sin corrección disponible o con riesgo compensado, con justificación
y fecha de vencimiento. Al vencer, el control vuelve a fallar y la excepción se re-evalúa.

- Dependencias: [`.snyk`](.snyk)
- Configuración: [`.trivyignore.yaml`](.trivyignore.yaml)

## Credenciales

- Las credenciales de la base de datos se generan en cada despliegue y viven solo en el Secret
  `mysql-credentials` de Kubernetes; no están en el repositorio ni en los logs.
- Los tokens del pipeline (`SONAR_TOKEN`, `SNYK_TOKEN`) son GitHub Secrets.
- `.env` está en `.gitignore`; la plantilla sin valores reales es `.env.example`.
