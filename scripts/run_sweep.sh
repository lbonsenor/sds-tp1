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

# Helper function to render a 30-character progress bar
draw_progress_bar() {
    local current=$1
    local total=$2
    local bar_width=30

    local percentage=$(( current * 100 / total ))
    local filled=$(( current * bar_width / total ))
    local empty=$(( bar_width - filled ))

    # Build bar visual using block characters
    local bar=""
    for ((i = 0; i < filled; i++)); do bar+="█"; done
    for ((i = 0; i < empty; i++)); do bar+="░"; done

    # \r resets cursor to beginning of line; \033[K clears to the end of the line
    printf "\rProgress: [%s] %3d%% (%d/%d)" "${bar}" "${percentage}" "${current}" "${total}"
}

echo "Se ejecutarán ${TOTAL} configuraciones desde ${CONFIG_DIR}"
echo "La telemetría se acumulará en ${REPOSITORY_ROOT}/telemetry"
echo ""

for INDEX in "${!CONFIGS[@]}"; do
    CONFIG_PATH="${CONFIGS[INDEX]}"
    RELATIVE_CONFIG="${CONFIG_PATH#"${REPOSITORY_ROOT}/"}"
    CURRENT=$((INDEX + 1))

    # Draw updated progress before starting the job
    draw_progress_bar "$((CURRENT - 1))" "${TOTAL}"

    # Run maven command with output suppressed or redirected to preserve progress bar alignment
    if (
        cd "${REPOSITORY_ROOT}"
        mvn compile exec:java "-Dexec.args=@${RELATIVE_CONFIG}"
    ) > /dev/null 2>&1; then
        SUCCEEDED=$((SUCCEEDED + 1))
    else
        FAILED=$((FAILED + 1))
        # Clear line to display error cleanly above the progress bar state
        printf '\r\033[KError en: %s\n' "${RELATIVE_CONFIG}" >&2
    fi
done

# Final progress update to ensure 100% is displayed
draw_progress_bar "${TOTAL}" "${TOTAL}"
printf '\n\nFinalizado: %d exitosas, %d fallidas.\n' "${SUCCEEDED}" "${FAILED}"

if ((FAILED > 0)); then
    exit 1
fi