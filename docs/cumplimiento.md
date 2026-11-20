# Matriz de cumplimiento y auditoría

Relación entre cada control automático del pipeline `CI/CD Microservicio Peliculas`
(`.github/workflows/ci.yml`), su umbral, cómo bloquea y dónde queda la evidencia.

## 1. Controles del pipeline

| # | Control | Job / paso | Herramienta | Umbral | Si no se cumple | Evidencia | Referencia |
|---|---|---|---|---|---|---|---|
| 1 | Pruebas automatizadas | `gate-tests-cobertura` | Maven Surefire | 0 tests fallidos | `mvn verify` exit 1; no corre ningún job posterior | Artefacto `reportes-pruebas` (30 días), resumen del run | ISO/IEC 27001:2022 A.8.29 |
| 2 | Cobertura de código | `gate-tests-cobertura` (`jacoco:check`) | JaCoCo 0.8.14 | Líneas y ramas ≥ 80 % (`cobertura.minima` en `pom.xml`) | `mvn verify` exit 1 | `target/site/jacoco` en `reportes-pruebas`, métrica `ci_cobertura_*` en Grafana | ISO/IEC 27001:2022 A.8.29 |
| 3 | Análisis estático (SAST) | `gate-sonarcloud` | SonarCloud | Quality Gate aprobado (`sonar.qualitygate.wait=true`) | Exit 1; bloquea `imagen-docker` | Comentario en el PR, proyecto en SonarCloud | ISO/IEC 27001:2022 A.8.28 |
| 4 | Dependencias vulnerables (SCA) | `gate-snyk` | Snyk CLI | 0 high/critical (`--severity-threshold=high`) | Exit 1 (vulnerabilidades) o 2 (error del escáner); bloquea `imagen-docker` y el despliegue | Pestaña Security (categoría `snyk-sca`), métrica `ci_vulnerabilidades_altas` | ISO/IEC 27001:2022 A.8.8 |
| 5 | Secretos en el código | `gate-auditoria` | Gitleaks 8.30.1 | 0 hallazgos en los commits del cambio | Exit 1; bloquea `imagen-docker` | Pestaña Security (`gitleaks`), artefacto `reportes-auditoria-seguridad` (90 días) | ISO/IEC 27001:2022 A.5.17 |
| 6 | Configuración insegura | `gate-auditoria` | Trivy 0.69.3 (`config`) | 0 HIGH/CRITICAL en Dockerfile y manifiestos | Exit 1; bloquea `imagen-docker` | Pestaña Security (`trivy-config`), `reportes-auditoria-seguridad` | CIS Docker / Kubernetes Benchmark, ISO/IEC 27001:2022 A.8.9 |
| 7 | Vulnerabilidades de la imagen | `imagen-docker` | Trivy 0.69.3 (`image`) | 0 HIGH/CRITICAL con corrección disponible | Exit 1; la imagen no se publica | Pestaña Security (`trivy-imagen`) | ISO/IEC 27001:2022 A.8.8 |
| 8 | Despliegue verificado | `despliegue-k8s` | kind + `smoke-test.sh` | Rollout completo y 9 verificaciones (HTTP, métrica de versión, Prometheus, MySQL, Pushgateway, Loki) | Rollback a `stable`; la versión no se promueve; exit 1 | Artefacto `evidencia-despliegue` (90 días), `historial.csv` en rama `metrics-history` | ISO/IEC 27001:2022 A.8.32 |
| 9 | Protección de la rama `main` | `reporte-auditoria` | `auditar-proteccion-rama.sh` | Ruleset activo: PR obligatorio, 5 checks requeridos, sin force-push ni borrado | Advertencia en el run y fila "Desviación" en el reporte | Artefacto `reporte-auditoria-<n>` (90 días) | ISO/IEC 27001:2022 A.8.4, A.8.32 |

Cada ejecución publica el **reporte de auditoría** (`scripts/reporte-auditoria.sh`) en el resumen
del run y como artefacto `reporte-auditoria-<número>` con retención de 90 días.

## 2. Gobernanza del repositorio

Se aplica con `./scripts/configurar-repositorio.sh` (idempotente). La definición vive en el
repositorio para que los cambios de la política también pasen por PR.

| Control | Configuración | Archivo / comando |
|---|---|---|
| Protección de `main` y `develop` | Cambios solo por PR (merge commit), conversaciones resueltas, checks requeridos: `gate-tests-cobertura`, `gate-sonarcloud`, `gate-snyk`, `gate-auditoria`, `imagen-docker` (emitidos por GitHub Actions), sin force-push ni borrado, sin bypass | `.github/rulesets/proteccion-ramas.json` |
| Secret scanning + push protection | Rechaza el push que contiene un secreto conocido | `configurar-repositorio.sh` |
| Alertas y actualizaciones de seguridad | Dependabot alerts + security updates | `configurar-repositorio.sh` |
| Actualizaciones de versión | Maven, GitHub Actions y Docker, semanal, PR hacia `develop` | `.github/dependabot.yml` |
| Reporte privado de vulnerabilidades | Security → Report a vulnerability | `SECURITY.md` |
| Revisión del cambio | Plantilla con checklist de verificación y seguridad | `.github/pull_request_template.md` |

Verificación manual de la protección (misma consulta que hace el pipeline):

```bash
./scripts/auditar-proteccion-rama.sh Daniloko12/devops_007d_ols main
./scripts/auditar-proteccion-rama.sh Daniloko12/devops_007d_ols develop
```

## 3. Registro de excepciones

| Herramienta | Identificador | Alcance | Motivo | Vence |
|---|---|---|---|---|
| Snyk | `SNYK-JAVA-ORGGLASSFISHJAXB-20883142` (CVE-2026-92877, XXE) | `org.glassfish.jaxb:jaxb-core` (transitiva de Hibernate) | Sin versión corregida publicada; la API no deserializa XML | 2027-02-28 |
| Trivy config | `AVD-KSV-0014` / `KSV014` (readOnlyRootFilesystem) | `k8s/mysql.yaml` | La imagen de MySQL escribe fuera del volumen de datos; corre sin root, sin capacidades y con seccomp | 2027-02-28 |

Las excepciones están en `.snyk` y `.trivyignore.yaml`. Al vencer, el control correspondiente
vuelve a fallar.

## 4. Retención de evidencia

| Evidencia | Dónde | Retención |
|---|---|---|
| Reporte de auditoría por ejecución | Artefacto `reporte-auditoria-<n>` + resumen del run | 90 días |
| Resultados SAST/SCA/secretos/configuración | Pestaña Security (code scanning, SARIF) | Mientras exista la alerta |
| Reportes de seguridad (JSON/SARIF) | Artefacto `reportes-auditoria-seguridad` | 90 días |
| Pruebas y cobertura | Artefacto `reportes-pruebas` | 30 días |
| Estado del clúster y logs del despliegue | Artefacto `evidencia-despliegue` | 90 días |
| Métricas por ejecución en `main` | `historial.csv` en la rama `metrics-history` | Permanente (historial git) |
| Trazabilidad de versiones desplegadas | `kubectl rollout history` (change-cause = imagen) y tags `sha-<commit>` en GHCR | Permanente en GHCR |
