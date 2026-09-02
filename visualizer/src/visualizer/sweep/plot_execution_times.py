from __future__ import annotations

from pathlib import Path
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from .common import DENSITY_COLORS, number_label, save_figure, std_or_zero
from .telemetry import TelemetryBundle

# Step 0 execution time benchmarks (ms) mapped by density for L=50
# rho = 0.16 (N=400) -> 0.001881324 s = 1.881324 ms
# rho = 0.40 (N=1000) -> 0.003026313 s = 3.026313 ms
DENSITY_TIME_BENCHMARKS: dict[float, float] = {
    0.16: 1.881324,
    0.40: 3.026313,
}


def _plot_model_timings(
    model_name: str,
    df_model: pd.DataFrame,
    output_dir: Path,
    image_format: str,
    dpi: int,
) -> Path:
    """Generates a step vs. execution time (ms) plot for a single model."""
    figure, axis = plt.subplots(figsize=(7.6, 5.0))

    # Aggregate by step and density across runs/replicas to get mean & std
    summary = (
        df_model.groupby(["density", "step"], as_index=False)
        .agg(
            mean_time_ms=("execution_time_ms", "mean"),
            std_time_ms=("execution_time_ms", std_or_zero),
        )
        .sort_values("step")
    )

    model_key = model_name.lower()

    # Plot actual run data grouped by density
    densities = sorted(summary["density"].unique())
    for index, density in enumerate(densities):
        density_float = float(density)
        curve = summary[summary["density"] == density]
        color = DENSITY_COLORS[index % len(DENSITY_COLORS)]

        # Line plot for measured execution time vs step
        axis.plot(
            curve["step"],
            curve["mean_time_ms"],
            label=f"$\\rho$={number_label(density_float)}",
            color=color,
            linestyle="-",
            alpha=0.8,
        )

        # Horizontal benchmark lines based on Step 0 averages
        if density_float in DENSITY_TIME_BENCHMARKS:
            benchmark_val = DENSITY_TIME_BENCHMARKS[density_float]
            axis.axhline(
                y=benchmark_val,
                color=color,
                linestyle="--",
                linewidth=1.2,
                label=f"Promedio Paso 0 ($\\rho$={number_label(density_float)})",
            )

    axis.set_xlabel("Paso (step)")
    axis.set_ylabel("Tiempo de ejecución (ms)")
    # axis.set_title(f"Tiempo de ejecución del CIM por paso ({model_name.capitalize()})")
    axis.legend(fontsize=8, loc="upper right")
    figure.tight_layout()

    filename = f"tiempos_ejecucion_cim_{model_key.replace(' ', '_')}"
    return save_figure(figure, output_dir, filename, image_format, dpi)


def generate(
    bundle: TelemetryBundle,
    output_dir: Path,
    image_format: str,
    dpi: int,
    tp1_execution_csv: Path | None = None,
) -> list[Path]:
    """Generates step vs time plots filtered for L=50 and N in (400, 1000)."""
    if bundle.execution_times.empty:
        return []

    df = bundle.execution_times.copy()

    # Normalize column names for robust filtering
    df.columns = df.columns.str.lower()

    # Map step column if named differently in dataframe schema
    if "step" not in df.columns and "n_steps" in df.columns:
        df["step"] = df["n_steps"]

    # Filter for L=50 and N in (400, 1000)
    box_col = next((col for col in ["box_length", "l", "length"] if col in df.columns), None)
    n_col = next((col for col in ["n_particles", "n"] if col in df.columns), None)

    if box_col is not None:
        df = df[np.isclose(df[box_col], 50.0)]

    if n_col is not None:
        df = df[df[n_col].isin([400, 1000])]

    if df.empty:
        return []

    output_files: list[Path] = []

    # Generate separate graphs for Standard and Voter models
    for model_key in ["standard", "voter"]:
        model_df = df[df["model"].str.lower() == model_key]
        if not model_df.empty:
            saved_file = _plot_model_timings(
                model_name=model_key,
                df_model=model_df,
                output_dir=output_dir,
                image_format=image_format,
                dpi=dpi,
            )
            output_files.append(saved_file)

    return output_files