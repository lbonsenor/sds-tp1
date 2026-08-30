"""Figure (g): tiempo medio por paso del CIM vs. N, comparado con TP1.

TP2's own timings come from ``execution_times_cim.csv`` through the shared
``ExecutionTimeParser`` (same schema/dataclass used everywhere else in this
package). The enunciado additionally asks to contrast these with TP1's
timings; TP1 predates this parser package and its CSV schema is not fixed
(different column names for N and the timing units), so that one file is
read with a small, explicitly-documented column-sniffer instead of forcing
it through ``ExecutionTimeRecord``.
"""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from .common import DENSITY_COLORS, errorbar, model_label, number_label, save_figure, std_or_zero
from .telemetry import TelemetryBundle


def _tp1_execution_frame(csv_path: Path) -> pd.DataFrame:
    """Accept the common TP1 timing schemas and return N, mean, and std in ms."""
    table = pd.read_csv(csv_path)
    table.columns = table.columns.str.strip().str.lower()
    n_column = next((column for column in ("n_particles", "n") if column in table.columns), None)
    ms_column = next(
        (column for column in ("mean_time_ms", "execution_time_ms", "time_ms") if column in table.columns),
        None,
    )
    seconds_column = next(
        (column for column in ("mean_time_sec", "execution_time_sec", "time_sec") if column in table.columns),
        None,
    )
    std_ms_column = next(
        (column for column in ("std_dev_ms", "std_time_ms", "time_std_ms") if column in table.columns),
        None,
    )
    std_seconds_column = next(
        (column for column in ("std_dev_sec", "std_time_sec", "time_std_sec") if column in table.columns),
        None,
    )
    if n_column is None or (ms_column is None and seconds_column is None):
        raise ValueError(
            "El CSV de TP1 debe tener N/n_particles y una columna de tiempo en ms o sec "
            "(por ejemplo mean_time_ms)."
        )

    time_ms = table[ms_column] if ms_column is not None else table[seconds_column] * 1000.0
    if std_ms_column is not None:
        reported_std_ms = table[std_ms_column]
    elif std_seconds_column is not None:
        reported_std_ms = table[std_seconds_column] * 1000.0
    else:
        reported_std_ms = pd.Series(np.nan, index=table.index)

    data = pd.DataFrame(
        {
            "n_particles": pd.to_numeric(table[n_column], errors="raise"),
            "time_ms": pd.to_numeric(time_ms, errors="raise"),
            "reported_std_ms": pd.to_numeric(reported_std_ms, errors="coerce"),
        }
    )
    summary = (
        data.groupby("n_particles", as_index=False)
        .agg(
            mean_time_ms=("time_ms", "mean"),
            between_run_std_ms=("time_ms", std_or_zero),
            mean_reported_variance=(
                "reported_std_ms",
                lambda values: float(np.nanmean(np.square(values))) if values.notna().any() else 0.0,
            ),
        )
        .sort_values("n_particles")
    )
    summary["std_time_ms"] = np.sqrt(
        np.square(summary.pop("between_run_std_ms")) + summary.pop("mean_reported_variance")
    )
    return summary


def generate(
    bundle: TelemetryBundle,
    output_dir: Path,
    image_format: str,
    dpi: int,
    tp1_execution_csv: Path | None,
) -> list[Path]:
    if bundle.execution_times.empty:
        return []

    per_run = bundle.execution_times.groupby(
        ["run_id", "model", "density", "n_particles", "method"], as_index=False
    ).agg(time_ms=("execution_time_ms", "mean"))
    summary = (
        per_run.groupby(["model", "density", "n_particles", "method"], as_index=False)
        .agg(
            mean_time_ms=("time_ms", "mean"),
            std_time_ms=("time_ms", std_or_zero),
            replicas=("run_id", "nunique"),
        )
        .sort_values("n_particles")
    )

    figure, axis = plt.subplots(figsize=(7.6, 5.0))
    plotted_n_particles = set(summary["n_particles"].astype(float))
    for index, ((model, density, method), curve) in enumerate(
        summary.groupby(["model", "density", "method"], sort=True)
    ):
        model_str = str(model)
        density_float = float(density)  # type: ignore[arg-type]
        errorbar(
            axis,
            curve["n_particles"],
            curve["mean_time_ms"],
            curve["std_time_ms"],
            label=f"TP2 {model_label(model_str)}, $\\rho={number_label(density_float)}$ ({method})",
            color=DENSITY_COLORS[index % len(DENSITY_COLORS)],
            marker="o",
            linestyle="-",
        )

    if tp1_execution_csv is not None:
        tp1_data = _tp1_execution_frame(tp1_execution_csv)
        plotted_n_particles.update(tp1_data["n_particles"].astype(float))
        errorbar(
            axis,
            tp1_data["n_particles"],
            tp1_data["mean_time_ms"],
            tp1_data["std_time_ms"],
            label="TP1",
            color="#444444",
            marker="s",
            linestyle="--",
        )
    else:
        bundle.warnings.append(
            "La figura de tiempos muestra solo TP2: pasá --tp1-execution-csv para agregar la "
            "comparación con TP1 que pide el punto (g) del enunciado."
        )

    axis.set_xlabel("Número de partículas $N$")
    axis.set_ylabel("Tiempo medio por paso [ms]")
    axis.grid(True, alpha=0.28)
    axis.legend(fontsize=8)
    axis.set_title("Tiempo de ejecución del CIM")
    if len(plotted_n_particles) > 1:
        axis.set_xscale("log")
        axis.set_yscale("log")
    figure.tight_layout()
    return [save_figure(figure, output_dir, "tiempos_ejecucion_cim", image_format, dpi)]