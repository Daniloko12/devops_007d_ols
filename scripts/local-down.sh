#!/usr/bin/env bash
# Elimina el clúster kind local con todo su contenido (app, MySQL, monitoreo y datos).
set -euo pipefail
kind delete cluster --name ep3
