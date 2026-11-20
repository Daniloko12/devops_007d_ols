#!/usr/bin/env bash
# Asegura que el usuario "exporter" exista en el MySQL en ejecución (idempotente).
#
# Por qué: MySQL ejecuta los scripts de /docker-entrypoint-initdb.d una sola vez, al
# inicializar un volumen vacío. Si el contenedor se reinicia a mitad de esa inicialización,
# al volver encuentra la base de datos creada, se salta los scripts y el usuario del
# exporter no se crea: Prometheus recibe mysql_up = 0 aunque MySQL funcione.
# Se detectó en el pipeline (mysql-0 con RESTARTS 1) y por eso se ejecuta en cada despliegue.
#
# Las contraseñas no pasan por este script: el SQL corre dentro del contenedor y lee
# las variables de entorno que vienen del Secret mysql-credentials.
set -euo pipefail

NS=peliculas

kubectl -n "$NS" rollout status statefulset/mysql --timeout=300s
kubectl -n "$NS" exec mysql-0 -c mysql -- /docker-entrypoint-initdb.d/01-usuario-exporter.sh
