"""Load TP2 telemetry through the typed CSV parsers and build the summaries
shared by every plot in the ``sweep`` package.

This module parses and validates ``time_observables.csv``, ``run_config.csv``
and ``execution_times_cim.csv`` into strongly-typed DataFrames and computes
steady-state statistics and convergence diagnostics.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Final, cast

import numpy as np
import pandas as pd

from visualizer.parser import ExecutionTimeParser, RunConfigParser, TimeObservableParser

from .common import normalise_model, records_to_frame, std_or_zero

CONDITION_COLUMNS: Final[list[str]] = ["model", "density", "eta"]

# Diagnóstico de percolación espacial (S saturado cerca de 1)
PERCOLATION_SPAN_THRESHOLD: Final[float] = 0.05
PERCOLATION_LEVEL_THRESHOLD: Final[float] = 0.95

# Diagnóstico de falta de convergencia / derretimiento transitorio (en unidades de desvío estándar)
CONVERGENCE_TREND_THRESHOLD: Final[float] = 1.5

# Parámetros para la detección dinámica del estado estacionario
ROLLING_WINDOW_FRACTION: Final[float] = 0.10  # Tamaño de la ventana móvil (10% de la serie)
SLOPE_TOLERANCE: Final[float] = 1e-4          # Tolerancia para considerar la pendiente como plana


@dataclass
class TelemetryBundle:
    """Contenedor de todos los DataFrames procesados e información de diagnóstico."""

    observables: pd.DataFrame = field(default_factory=pd.DataFrame)
    run_configs: pd.DataFrame = field(default_factory=pd.DataFrame)
    execution_times: pd.DataFrame = field(default_factory=pd.DataFrame)
    steady_by_run: pd.DataFrame = field(default_factory=pd.DataFrame)
    steady_summary: pd.DataFrame = field(default_factory=pd.DataFrame)
    temporal_summary: pd.DataFrame = field(default_factory=pd.DataFrame)
    temporal_starts: pd.DataFrame = field(default_factory=pd.DataFrame)
    warnings: list[str] = field(default_factory=list)

    def temporal_start(self, model: str, density: float, eta: float) -> float | None:
        """Retorna el tiempo de inicio del estado estacionario para una condición dada."""
        rows: pd.DataFrame = self.temporal_starts.loc[
            (self.temporal_starts["model"] == model)
            & np.isclose(self.temporal_starts["density"], density)
            & np.isclose(self.temporal_starts["eta"], eta)
        ]
        return None if rows.empty else float(rows.iloc[0]["steady_state_start"])


def load_telemetry(telemetry_dir: Path, fallback_fraction: float = 0.5) -> TelemetryBundle:
    """Carga y procesa `time_observables.csv` junto con los CSVs auxiliares opcionales."""
    bundle: TelemetryBundle = TelemetryBundle()
    telemetry_path: Path = Path(telemetry_dir)

    observables_path: Path = telemetry_path / "time_observables.csv"
    if not observables_path.exists():
        raise FileNotFoundError(
            f"No se encontró {observables_path}. Se requiere para las figuras b-e."
        )

    observables: pd.DataFrame = records_to_frame(TimeObservableParser(observables_path).load())
    if observables.empty:
        raise ValueError(f"{observables_path} no contiene observables temporales.")

    observables["model"] = normalise_model(observables["model"])
    for column in ("density", "eta", "time", "va", "cluster_ratio"):
        observables[column] = pd.to_numeric(observables[column], errors="raise")
    bundle.observables = observables

    config_path: Path = telemetry_path / "run_config.csv"
    if config_path.exists():
        configs: pd.DataFrame = records_to_frame(RunConfigParser(config_path).load())
        configs["model"] = normalise_model(configs["model"])
        bundle.run_configs = configs
    else:
        bundle.warnings.append(
            "No se encontró run_config.csv; se infiere el final de cada corrida desde "
            "time_observables.csv."
        )

    execution_path: Path = telemetry_path / "execution_times_cim.csv"
    if execution_path.exists():
        execution_times: pd.DataFrame = records_to_frame(ExecutionTimeParser(execution_path).load())
        if not execution_times.empty:
            execution_times["model"] = normalise_model(execution_times["model"])
            bundle.execution_times = execution_times
    else:
        bundle.warnings.append(
            "No se encontró execution_times_cim.csv; se omitirá la figura de tiempos del CIM."
        )

    _build_summaries(bundle, fallback_fraction)
    _diagnose_percolation(bundle)
    _diagnose_convergence(bundle)
    return bundle


def _run_end_times(bundle: TelemetryBundle) -> pd.Series:
    """Determina el tiempo final de cada simulación."""
    inferred: pd.Series = bundle.observables.groupby("run_id")["time"].transform("max")
    if bundle.run_configs.empty or "total_time" not in bundle.run_configs.columns:
        return inferred

    configured: pd.Series = bundle.run_configs.drop_duplicates("run_id").set_index("run_id")["total_time"]
    mapped: pd.Series = bundle.observables["run_id"].map(configured)
    missing: pd.Series = mapped.isna()
    if missing.any():
        bundle.warnings.append(
            f"{int(missing.sum())} filas de time_observables.csv no tienen su run_id en "
            "run_config.csv; se usó el tiempo máximo observado."
        )
        mapped = mapped.where(~missing, inferred)
    return mapped.astype(float)


def _detect_steady_state_start(
    time_series: pd.Series,
    observable_series: pd.Series,
    fallback_fraction: float,
) -> float:
    """Calcula el inicio del estado estacionario detectando la estabilización de la pendiente móvil.

    Si no logra convergencia antes del final de la serie, retorna un respaldo basado en `fallback_fraction`.
    """
    n_samples: int = len(observable_series)
    if n_samples < 10:
        return float(time_series.iloc[0])

    window_size: int = max(5, int(n_samples * ROLLING_WINDOW_FRACTION))

    dt: pd.Series = time_series.diff()
    dy: pd.Series = observable_series.diff()

    valid_dt: pd.Series = dt.replace(0, np.nan)
    derivative: pd.Series = (dy / valid_dt).fillna(0.0)

    rolling_slope: pd.Series = derivative.rolling(window=window_size, min_periods=window_size).mean().abs()

    stable_indices: pd.Series = rolling_slope < SLOPE_TOLERANCE
    if stable_indices.any():
        first_stable_idx: int = int(stable_indices.idxmax())
        return float(time_series.loc[first_stable_idx])

    max_time: float = float(time_series.max())
    return max_time * fallback_fraction


def _build_summaries(bundle: TelemetryBundle, fallback_fraction: float) -> None:
    """Calcula promedios temporales y promedios en el estado estacionario dinámico."""
    # Redondeo de eta para evitar desalineación flotante
    bundle.observables["eta"] = bundle.observables["eta"].round(6)

    # 1. Promedio de ensamble temporal para evaluar la convergencia por condición
    bundle.temporal_summary = (
        bundle.observables.groupby([*CONDITION_COLUMNS, "time"], as_index=False)
        .agg(
            va_mean=("va", "mean"),
            va_std=("va", std_or_zero),
            s_mean=("cluster_ratio", "mean"),
            s_std=("cluster_ratio", std_or_zero),
            replicas=("run_id", "nunique"),
        )
        .sort_values([*CONDITION_COLUMNS, "time"])
    )

    # 2. Detección dinámica e independiente por modelo del tiempo de inicio del estado estacionario
    starts_list: list[dict[str, str | float]] = []
    for keys_tuple, group in bundle.temporal_summary.groupby(CONDITION_COLUMNS):
        raw_model, raw_density, raw_eta = cast("tuple[Any, Any, Any]", keys_tuple)
        model: str = str(raw_model)
        density: float = float(raw_density)
        eta: float = float(raw_eta)

        group_sorted: pd.DataFrame = group.sort_values("time")

        start_time: float = _detect_steady_state_start(
            time_series=group_sorted["time"],
            observable_series=group_sorted["va_mean"],
            fallback_fraction=fallback_fraction,
        )

        starts_list.append({
            "model": model,
            "density": density,
            "eta": eta,
            "steady_state_start": start_time,
        })

    bundle.temporal_starts = pd.DataFrame(starts_list).sort_values(CONDITION_COLUMNS)

    # 3. Asignación del tiempo de inicio específico a cada registro original
    bundle.observables = bundle.observables.merge(
        bundle.temporal_starts,
        on=CONDITION_COLUMNS,
        how="left",
    )

    # 4. Cálculo de agregaciones en el régimen estacionario
    run_columns: list[str] = ["run_id", *CONDITION_COLUMNS]
    steady_rows: pd.DataFrame = bundle.observables.loc[
        bundle.observables["time"] >= bundle.observables["steady_state_start"]
    ]

    bundle.steady_by_run = (
        steady_rows.groupby(run_columns, as_index=False)
        .agg(va=("va", "mean"), cluster_ratio=("cluster_ratio", "mean"))
        .sort_values(CONDITION_COLUMNS)
    )

    bundle.steady_summary = (
        bundle.steady_by_run.groupby(CONDITION_COLUMNS, as_index=False)
        .agg(
            va_mean=("va", "mean"),
            va_std=("va", std_or_zero),
            s_mean=("cluster_ratio", "mean"),
            s_std=("cluster_ratio", std_or_zero),
            replicas=("run_id", "nunique"),
        )
        .sort_values(CONDITION_COLUMNS)
    )


def _diagnose_percolation(bundle: TelemetryBundle) -> None:
    """Advierte cuando S casi no varía con η y permanece cercano a 1 por percolación espacial."""
    if bundle.steady_summary.empty:
        return
    for (model, density), group in bundle.steady_summary.groupby(["model", "density"]):
        span: float = float(group["s_mean"].max() - group["s_mean"].min())
        level: float = float(group["s_mean"].mean())
        if span < PERCOLATION_SPAN_THRESHOLD and level > PERCOLATION_LEVEL_THRESHOLD:
            bundle.warnings.append(
                f"S casi no varía con eta (~{level:.2f}, rango {span:.3f}) para "
                f"modelo={model}, rho={density:g}: revisar el radio de conexión r_c "
                "frente a la densidad. En rho alta el sistema percola geométricamente."
            )


def _diagnose_convergence(bundle: TelemetryBundle) -> None:
    """Advierte si la ventana del estado estacionario seleccionada todavía presenta deriva (tanto en v_a como en S)."""
    if bundle.observables.empty:
        return
    steady_rows: pd.DataFrame = bundle.observables.loc[
        bundle.observables["time"] >= bundle.observables["steady_state_start"]
    ]
    for keys, group in steady_rows.groupby(CONDITION_COLUMNS):
        group = group.sort_values("time")
        midpoint: int = len(group) // 2
        if midpoint < 3:
            continue

        for var_name in ("va", "cluster_ratio"):
            first_half: pd.Series = group.iloc[:midpoint][var_name]
            second_half: pd.Series = group.iloc[midpoint:][var_name]
            if len(first_half) < 2 or len(second_half) < 2:
                continue
            pooled_std: float = float(np.sqrt(first_half.var(ddof=1) + second_half.var(ddof=1)))
            if not np.isfinite(pooled_std) or pooled_std == 0.0:
                continue
            drift: float = abs(float(second_half.mean()) - float(first_half.mean()))
            if drift > CONVERGENCE_TREND_THRESHOLD * pooled_std:
                model, density, eta = keys
                bundle.warnings.append(
                    f"El observable {var_name} todavía muestra tendencia (no convergencia) en "
                    f"modelo={model}, rho={density:g}, eta={eta:g} (deriva={drift:.3f}). "
                    "Aumentar el tiempo total de simulación o descartar una ventana transitoria mayor."
                )