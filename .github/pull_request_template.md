## Descripción

<!-- Qué cambia y por qué. Si existe un issue relacionado, enlázalo (Closes #N). -->

## Tipo de cambio

- [ ] Corrección (`fix`)
- [ ] Funcionalidad nueva (`feat`)
- [ ] Refactorización sin cambio de comportamiento (`refactor`)
- [ ] Pipeline, Kubernetes o monitoreo (`ci`, `k8s`)
- [ ] Documentación (`docs`)

## Verificación local

- [ ] `./mvnw clean verify` termina en `BUILD SUCCESS` (tests en verde y cobertura ≥ 80 % de líneas y ramas)
- [ ] Si cambia `k8s/`, `monitoring/` o `scripts/`: `./scripts/local-up.sh` termina con "todas las verificaciones pasaron"

## Seguridad y cumplimiento

- [ ] No incluye secretos, claves ni archivos `.env` (Gitleaks lo verifica igual)
- [ ] Las dependencias nuevas o actualizadas no agregan vulnerabilidades high/critical (Snyk lo verifica igual)
- [ ] Si agrega o modifica una excepción en `.snyk` o `.trivyignore.yaml`, incluye justificación y fecha de vencimiento

## Controles automáticos (requeridos por el ruleset `proteccion-main-develop`)

El PR no se puede integrar si alguno falla:

| Check | Exige |
|---|---|
| `gate-tests-cobertura` | 0 tests fallidos y cobertura ≥ 80 % (JaCoCo) |
| `gate-sonarcloud` | Quality Gate de SonarCloud aprobado (se exige en PR hacia `main`) |
| `gate-snyk` | 0 vulnerabilidades high/critical en dependencias |
| `gate-auditoria` | 0 secretos (Gitleaks) y 0 configuraciones HIGH/CRITICAL (Trivy config) |
| `imagen-docker` | 0 vulnerabilidades HIGH/CRITICAL con corrección en la imagen (Trivy image) |
