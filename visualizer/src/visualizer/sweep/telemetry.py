"""Load TP2 telemetry through the typed CSV parsers and build the summaries
shared by every plot in the ``sweep`` package.

This module is the only place that touches ``time_observables.csv``,
``run_config.csv`` and ``execution_times_cim.csv`` directly -- and it always
does so through ``visualizer.parser``, never with ad-hoc ``pandas.read_csv``
calls, so every plot module downstream works from the same typed, validated
data.

Two diagnostics were added on top of the original TP2 script, both aimed at
issues spotted while reviewing the figures:

* :func:`_diagnose_percolation` flags a (model, density) pair when the giant
  component fraction ``S`` stays pinned near 1 across every eta tested. That
  is exactly what happens when the clustering radius is large enough,
  relative to the density, that the particle graph percolates regardless of
  how aligned the velocities are -- it makes figures (d) and (e) uninformative.
* :func:`_diagnose_convergence` flags a (model, density, eta) run whose
  recorded "steady state" window still trends (its second half differs from
  its first half by more than a few combined standard deviations). This is
  the pattern seen in the voter model at low eta, where polarization was
  still climbing at the end of the simulated window.

Neither diagnostic changes the data or silently "fixes" anything -- they just
turn a visual inspection into an explicit warning printed by ``__main__``.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
import pandas as pd

from visualizer.parser import ExecutionTimeParser, RunConfigParser, TimeObservableParser

from .common import normalise_model, records_to_frame, std_or_zero

CONDITION_COLUMNS = ["model", "density", "eta"]

# A (model, density) combination is flagged as "insensitive to eta" when S
# barely moves and stays close to 1 across every eta tested.
PERCOLATION_SPAN_THRESHOLD = 0.05
PERCOLATION_LEVEL_THRESHOLD = 0.95

# A run is flagged as "possibly not converged" when the mean of the second
# half of its steady-state window differs from the mean of the first half by
# more than this many combined standard deviations.
CONVERGENCE_TREND_THRESHOLD = 1.5


@dataclass
class TelemetryBundle:
    """Every DataFrame the sweep plots need, plus diagnostics found while loading."""

    observables: pd.DataFrame = field(default_factory=pd.DataFrame)
    run_configs: pd.DataFrame = field(default_factory=pd.DataFrame)
    execution_times: pd.DataFrame = field(default_factory=pd.DataFrame)
    steady_by_run: pd.DataFrame = field(default_factory=pd.DataFrame)
    steady_summary: pd.DataFrame = field(default_factory=pd.DataFrame)
    temporal_summary: pd.DataFrame = field(default_factory=pd.DataFrame)
    temporal_starts: pd.DataFrame = field(default_factory=pd.DataFrame)
    warnings: list[str] = field(default_factory=list)

    def temporal_start(self, model: str, density: float, eta: float) -> float | None:
        rows = self.temporal_starts.loc[
            (self.temporal_starts["model"] == model)
            & np.isclose(self.temporal_starts["density"], density)
            & np.isclose(self.temporal_starts["eta"], eta)
        ]
        return None if rows.empty else float(rows.iloc[0]["steady_state_start"])


def load_telemetry(telemetry_dir: Path, steady_state_fraction: float) -> TelemetryBundle:
    """Load ``time_observables.csv`` (required) plus the optional companion CSVs."""
    bundle = TelemetryBundle()
    telemetry_dir = Path(telemetry_dir)

    observables_path = telemetry_dir / "time_observables.csv"
    if not observables_path.exists():
        raise FileNotFoundError(
            f"No se encontró {observables_path}. Se requiere para las figuras b-e."
        )

    observables = records_to_frame(TimeObservableParser(observables_path).load())
    if observables.empty:
        raise ValueError(f"{observables_path} no contiene observables temporales.")

    observables["model"] = normalise_model(observables["model"])
    for column in ("density", "eta", "time", "va", "cluster_ratio"):
        observables[column] = pd.to_numeric(observables[column], errors="raise")
    bundle.observables = observables

    config_path = telemetry_dir / "run_config.csv"
    if config_path.exists():
        configs = records_to_frame(RunConfigParser(config_path).load())
        configs["model"] = normalise_model(configs["model"])
        bundle.run_configs = configs
    else:
        bundle.warnings.append(
            "No se encontró run_config.csv; se infiere el final de cada corrida desde "
            "time_observables.csv y no se puede diagnosticar el radio de conexión de los "
            "clusters."
        )

    execution_path = telemetry_dir / "execution_times_cim.csv"
    if execution_path.exists():
        execution_times = records_to_frame(ExecutionTimeParser(execution_path).load())
        if not execution_times.empty:
            execution_times["model"] = normalise_model(execution_times["model"])
        bundle.execution_times = execution_times
    else:
        bundle.warnings.append(
            "No se encontró execution_times_cim.csv; se omitirá la figura de tiempos del CIM."
        )

    _build_summaries(bundle, steady_state_fraction)
    _diagnose_percolation(bundle)
    _diagnose_convergence(bundle)
    return bundle


def _run_end_times(bundle: TelemetryBundle) -> pd.Series:
    """Prefer the configured ``total_time`` per run_id; fall back to the max observed time.

    The original script loaded ``run_config.csv`` only to validate its schema
    and then inferred the end of each run from the observable telemetry
    itself. That silently assumes every run was recorded up to its actual
    configured length. Here we use the real ``total_time`` from
    ``run_config.csv`` whenever it is available, and only fall back to the
    inferred max recorded time (with an explicit warning) for runs missing
    from the config file.
    """
    inferred = bundle.observables.groupby("run_id")["time"].transform("max")
    if bundle.run_configs.empty or "total_time" not in bundle.run_configs.columns:
        return inferred

    configured = bundle.run_configs.drop_duplicates("run_id").set_index("run_id")["total_time"]
    mapped = bundle.observables["run_id"].map(configured)
    missing = mapped.isna()
    if missing.any():
        bundle.warnings.append(
            f"{int(missing.sum())} filas de time_observables.csv no tienen su run_id en "
            "run_config.csv; se usó el tiempo máximo observado en su lugar."
        )
        mapped = mapped.where(~missing, inferred)
    return mapped.astype(float)


def _build_summaries(bundle: TelemetryBundle, steady_state_fraction: float) -> None:
    run_end_time = _run_end_times(bundle)
    bundle.observables = bundle.observables.assign(
        steady_state_start=run_end_time * steady_state_fraction
    )
    run_columns = ["run_id", *CONDITION_COLUMNS]
    steady_rows = bundle.observables.loc[
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

    bundle.temporal_starts = (
        bundle.observables.groupby(CONDITION_COLUMNS, as_index=False)
        .agg(steady_state_start=("steady_state_start", "median"))
        .sort_values(CONDITION_COLUMNS)
    )


def _diagnose_percolation(bundle: TelemetryBundle) -> None:
    """Flag (model, density) pairs where S barely reacts to eta and stays near 1."""
    if bundle.steady_summary.empty:
        return
    for (model, density), group in bundle.steady_summary.groupby(["model", "density"]):
        span = float(group["s_mean"].max() - group["s_mean"].min())
        level = float(group["s_mean"].mean())
        if span < PERCOLATION_SPAN_THRESHOLD and level > PERCOLATION_LEVEL_THRESHOLD:
            bundle.warnings.append(
                f"S casi no varía con eta (~{level:.2f}, rango {span:.3f}) para "
                f"modelo={model}, rho={density:g}: revisar el radio de conexión de clusters "
                "(cut_off/min_radius/max_radius en run_config.csv) frente al espaciado "
                "interparticular a esa densidad; las figuras (d) y (e) pierden poder "
                "discriminante si S satura en 1 para todo eta."
            )


def _diagnose_convergence(bundle: TelemetryBundle) -> None:
    """Flag (model, density, eta) runs whose steady-state window still trends."""
    if bundle.observables.empty:
        return
    steady_rows = bundle.observables.loc[
        bundle.observables["time"] >= bundle.observables["steady_state_start"]
    ]
    for keys, group in steady_rows.groupby(CONDITION_COLUMNS):
        group = group.sort_values("time")
        midpoint = len(group) // 2
        if midpoint < 3:
            continue
        first_half = group.iloc[:midpoint]["va"]
        second_half = group.iloc[midpoint:]["va"]
        if len(first_half) < 2 or len(second_half) < 2:
            continue
        pooled_std = float(np.sqrt(first_half.var(ddof=1) + second_half.var(ddof=1)))
        if not np.isfinite(pooled_std) or pooled_std == 0.0:
            continue
        drift = abs(float(second_half.mean()) - float(first_half.mean()))
        if drift > CONVERGENCE_TREND_THRESHOLD * pooled_std:
            model, density, eta = keys
            bundle.warnings.append(
                "v_a todavía muestra tendencia dentro de la ventana estacionaria para "
                f"modelo={model}, rho={density:g}, eta={eta:g} (deriva={drift:.3f} vs. "
                f"{CONVERGENCE_TREND_THRESHOLD:g} desvíos combinados); considerar extender "
                "la simulación o reducir --steady-state-fraction para este caso."
            )
