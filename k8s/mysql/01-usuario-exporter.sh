#!/bin/bash
# Se ejecuta UNA sola vez: cuando MySQL inicializa un volumen vacío.
# Crea el usuario "exporter" con permisos de solo lectura sobre métricas
# (principio de mínimo privilegio): mysqld-exporter no puede modificar datos.
# EXPORTER_PASSWORD viene del Secret mysql-credentials (nunca está en git).
set -euo pipefail

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'exporter'@'localhost' IDENTIFIED BY '${EXPORTER_PASSWORD}' WITH MAX_USER_CONNECTIONS 3;
CREATE USER IF NOT EXISTS 'exporter'@'127.0.0.1' IDENTIFIED BY '${EXPORTER_PASSWORD}' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'localhost';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

echo "Usuario 'exporter' creado para mysqld-exporter"
