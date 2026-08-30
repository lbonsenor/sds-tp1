#!/usr/bin/env bash
# Run every TP2 sweep configuration sequentially from the repository root.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CONFIG_DIR="${1:-${REPOSITORY_ROOT}/configs/sweep}"

if [[ "${CONFIG_DIR}" != /* ]]; then
    CONFIG_DIR="${REPOSITORY_ROOT}/${CONFIG_DIR}"
fi

if [[ ! -d "${CONFIG_DIR}" ]]; then
    echo "Error: no existe el directorio de configuraciones: ${CONFIG_DIR}" >&2
    exit 2
fi

CONFIGS=("${CONFIG_DIR}"/*.txt)
if [[ ! -e "${CONFIGS[0]}" ]]; then
    echo "Error: no se encontraron archivos .txt en ${CONFIG_DIR}" >&2
    exit 2
fi

TOTAL=${#CONFIGS[@]}
SUCCEEDED=0
FAILED=0

echo "Se ejecutarán ${TOTAL} configuraciones desde ${CONFIG_DIR}"
echo "La telemetría se acumulará en ${REPOSITORY_ROOT}/telemetry"

for INDEX in "${!CONFIGS[@]}"; do
    CONFIG_PATH="${CONFIGS[INDEX]}"
    RELATIVE_CONFIG="${CONFIG_PATH#"${REPOSITORY_ROOT}/"}"
    CURRENT=$((INDEX + 1))

    printf '\n>>> [%d/%d] %s\n' "${CURRENT}" "${TOTAL}" "${RELATIVE_CONFIG}"
    if (
        cd "${REPOSITORY_ROOT}"
        mvn compile exec:java "-Dexec.args=@${RELATIVE_CONFIG}"
    ); then
        SUCCEEDED=$((SUCCEEDED + 1))
    else
        FAILED=$((FAILED + 1))
        echo "Falló: ${RELATIVE_CONFIG}" >&2
    fi
done

printf '\nFinalizado: %d exitosas, %d fallidas.\n' "${SUCCEEDED}" "${FAILED}"

if ((FAILED > 0)); then
    exit 1
fi
