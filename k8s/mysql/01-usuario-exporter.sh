#!/bin/bash
# Crea o actualiza el usuario "exporter" que usa mysqld-exporter, con permisos de solo
# lectura sobre métricas (principio de mínimo privilegio: no puede modificar datos).
#
# Es idempotente y se ejecuta en dos momentos:
#   1. Cuando MySQL inicializa un volumen vacío (/docker-entrypoint-initdb.d).
#   2. En cada despliegue (scripts/asegurar-usuario-exporter.sh, vía kubectl exec).
#      Así el usuario existe aunque el contenedor se haya reiniciado a mitad de la
#      inicialización (en ese caso MySQL no vuelve a ejecutar los scripts de inicio),
#      y la contraseña queda sincronizada con el Secret.
#
# Las claves vienen del Secret mysql-credentials como variables de entorno (nunca están
# en git). La clave de root se entrega con --defaults-extra-file, igual que el entrypoint
# oficial de MySQL, para que no aparezca en la línea de comandos.
set -euo pipefail

credenciales_root() {
  printf '[client]\npassword="%s"\n' "${MYSQL_ROOT_PASSWORD}"
}

# El socket se toma de la configuración del servidor (igual que el entrypoint oficial).
# No se usa la ruta por defecto del cliente: en la imagen mysql 8.0.46 el servidor escucha en
# /var/lib/mysql/mysql.sock y el cliente busca /var/run/mysqld/mysqld.sock (ERROR 2002).
# Durante la inicialización el servidor temporal no abre TCP, por eso se usa el socket.
SOCKET=""
for candidato in \
  "$(mysqld --verbose --help 2>/dev/null | awk '$1 == "socket" { print $2; exit }')" \
  /var/lib/mysql/mysql.sock /var/run/mysqld/mysqld.sock; do
  if [[ -n "$candidato" && -S "$candidato" ]]; then SOCKET="$candidato"; break; fi
done
[[ -n "$SOCKET" ]] || { echo "No se encontró el socket de MySQL"; exit 1; }

mysql --defaults-extra-file=<(credenciales_root) --protocol=socket --socket="$SOCKET" -uroot <<SQL
CREATE USER IF NOT EXISTS 'exporter'@'localhost' IDENTIFIED BY '${EXPORTER_PASSWORD}';
CREATE USER IF NOT EXISTS 'exporter'@'127.0.0.1' IDENTIFIED BY '${EXPORTER_PASSWORD}';
ALTER USER 'exporter'@'localhost' IDENTIFIED BY '${EXPORTER_PASSWORD}' WITH MAX_USER_CONNECTIONS 3;
ALTER USER 'exporter'@'127.0.0.1' IDENTIFIED BY '${EXPORTER_PASSWORD}' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'localhost';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'127.0.0.1';
SQL

echo "Usuario 'exporter' verificado para mysqld-exporter"
