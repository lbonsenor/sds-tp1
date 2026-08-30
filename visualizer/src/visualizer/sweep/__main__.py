"""Generate every static figure requested by TP2 from simulation telemetry.

Run as a module from the repository root::

    python3 -m visualizer.sweep

With the execution-time data from TP1 for the comparison requested in (g)::

    python3 -m visualizer.sweep --tp1-execution-csv path/to/tp1_execution_times.csv

Each figure lives in its own ``plot_*.py`` module under this package; this
file only wires the CLI, loads the telemetry once through
:mod:`visualizer.sweep.telemetry`, and calls each plot module in turn.
"""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Sequence

from . import (
    plot_cluster_ratio_vs_eta,
    plot_execution_times,
    plot_polarization_vs_cluster_ratio,
    plot_polarization_vs_eta,
    plot_temporal_cluster_ratio,
    plot_temporal_polarization,
)
from .telemetry import load_telemetry

REPOSITORY_ROOT = Path(__file__).resolve().parents[4]
DEFAULT_TELEMETRY_DIR = REPOSITORY_ROOT / "telemetry"
DEFAULT_OUTPUT_DIR = REPOSITORY_ROOT / "figures" / "tp2"


def _parse_eta_values(value: str) -> list[float]:
    try:
        values = [float(item.strip()) for item in value.split(",") if item.strip()]
    except ValueError as error:
        raise argparse.ArgumentTypeError(
            "--characteristic-etas debe ser una lista de números separada por comas."
        ) from error
    if not values:
        raise argparse.ArgumentTypeError("--characteristic-etas no puede estar vacío.")
    return values


def _build_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="python3 -m visualizer.sweep",
        description="Genera las figuras estáticas requeridas por el enunciado del TP2.",
    )
    parser.add_argument(
        "--telemetry-dir",
        type=Path,
        default=DEFAULT_TELEMETRY_DIR,
        help=f"Directorio con los CSV de telemetría (por defecto: {DEFAULT_TELEMETRY_DIR}).",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help=f"Directorio destino de las figuras (por defecto: {DEFAULT_OUTPUT_DIR}).",
    )
    parser.add_argument(
        "--steady-state-fraction",
        type=float,
        default=0.80,
        help="Fracción inicial de la corrida que se descarta antes de promediar (por defecto: 0.80).",
    )
    parser.add_argument(
        "--characteristic-etas",
        type=_parse_eta_values,
        default=[0.0, 3.0, 6.0],
        help="Etas a mostrar en las evoluciones temporales; usa el valor disponible más cercano.",
    )
    parser.add_argument(
        "--tp1-execution-csv",
        type=Path,
        help="CSV de tiempos del TP1 para agregarlo a la figura de tiempos del CIM (punto g).",
    )
    parser.add_argument(
        "--format",
        choices=("png", "pdf", "svg"),
        default="png",
        help="Formato de cada figura (por defecto: png).",
    )
    parser.add_argument("--dpi", type=int, default=300, help="Resolución de las imágenes rasterizadas.")
    return parser


def main(argv: Sequence[str] | None = None) -> None:
    args = _build_argument_parser().parse_args(argv)
    if not 0.0 < args.steady_state_fraction < 1.0:
        raise ValueError("--steady-state-fraction debe pertenecer al intervalo abierto (0, 1).")
    if args.dpi <= 0:
        raise ValueError("--dpi debe ser positivo.")
    if args.tp1_execution_csv is not None and not args.tp1_execution_csv.exists():
        raise FileNotFoundError(f"No se encontró el CSV de TP1: {args.tp1_execution_csv}")

    bundle = load_telemetry(args.telemetry_dir, args.steady_state_fraction)

    created_files: list[Path] = []
    created_files += plot_temporal_polarization.generate(
        bundle, args.output_dir, args.characteristic_etas, args.format, args.dpi
    )
    created_files += plot_polarization_vs_eta.generate(bundle, args.output_dir, args.format, args.dpi)
    created_files += plot_temporal_cluster_ratio.generate(
        bundle, args.output_dir, args.characteristic_etas, args.format, args.dpi
    )
    created_files += plot_cluster_ratio_vs_eta.generate(bundle, args.output_dir, args.format, args.dpi)
    created_files += plot_polarization_vs_cluster_ratio.generate(
        bundle, args.output_dir, args.format, args.dpi
    )
    created_files += plot_execution_times.generate(
        bundle, args.output_dir, args.format, args.dpi, args.tp1_execution_csv
    )

    for warning in bundle.warnings:
        print(f"Advertencia: {warning}")
    print(f"Se generaron {len(created_files)} figuras en {args.output_dir}:")
    for path in created_files:
        print(f"- {path.name}")


if __name__ == "__main__":
    main()
