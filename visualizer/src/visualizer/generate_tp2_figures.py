"""Generate every static figure requested by TP2 from simulation telemetry.

The script consumes the CSV files exported by the Java simulator and produces
one PNG per figure.  It intentionally aggregates the steady state in two
stages: first over time inside each replica, and then across replicas.  This
keeps the error bars from mixing temporal fluctuations with replica-to-replica
variability.

Examples
--------
From the repository root::

    python3 visualizer/src/visualizer/generate_tp2_figures.py

With the execution-time data from TP1 for the requested comparison::

    python3 visualizer/src/visualizer/generate_tp2_figures.py \
        --tp1-execution-csv path/to/tp1_execution_times.csv
"""

from __future__ import annotations

import argparse
from dataclasses import asdict
from pathlib import Path
import sys
from typing import Iterable, Sequence

import matplotlib

# Rendering is batch-only: the script must also work on machines without a GUI.
matplotlib.use("Agg")

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

# Make direct execution from the repository root work as well as `python -m`.
if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from visualizer.parser import ExecutionTimeParser, RunConfigParser, TimeObservableParser


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_TELEMETRY_DIR = REPOSITORY_ROOT / "telemetry"
DEFAULT_OUTPUT_DIR = REPOSITORY_ROOT / "figures" / "tp2"

MODEL_LABELS = {"standard": "Estándar", "voter": "Votante"}
MODEL_STYLES = {
    "standard": {"color": "#1f77b4", "marker": "o", "linestyle": "-"},
    "voter": {"color": "#d62728", "marker": "s", "linestyle": "--"},
}
DENSITY_COLORS = ["#1f77b4", "#ff7f0e", "#2ca02c", "#9467bd", "#8c564b"]


def _records_to_frame(records: Iterable[object]) -> pd.DataFrame:
    """Build a DataFrame from the typed records returned by the visualizer parsers."""
    return pd.DataFrame(asdict(record) for record in records)


def _normalise_model(values: pd.Series) -> pd.Series:
    """Keep model names stable despite the Java exporter using mixed case."""
    return values.astype(str).str.strip().str.lower().replace(
        {
            "vicsek": "standard",
            "standard": "standard",
            "voter": "voter",
            "votante": "voter",
        }
    )


def _number_label(value: float) -> str:
    return f"{float(value):g}"


def _safe_density_name(density: float) -> str:
    """Build a readable, filesystem-safe density fragment for a figure name."""
    return _number_label(density).replace("-", "neg").replace(".", "_")


def _ordered_models(values: Iterable[str]) -> list[str]:
    models = sorted(set(values))
    return sorted(models, key=lambda model: (model not in MODEL_LABELS, model))


def _model_label(model: str) -> str:
    return MODEL_LABELS.get(model, model.replace("_", " ").title())


def _model_style(model: str) -> dict[str, str]:
    if model in MODEL_STYLES:
        return MODEL_STYLES[model]
    fallback_index = sum(ord(character) for character in model) % len(DENSITY_COLORS)
    return {"color": DENSITY_COLORS[fallback_index], "marker": "^", "linestyle": "-"}


def _std_or_zero(values: pd.Series) -> float:
    std = float(values.std(ddof=1)) if len(values) > 1 else 0.0
    return 0.0 if np.isnan(std) else std


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


def _pick_characteristic_etas(
    available_etas: Sequence[float], requested_etas: Sequence[float]
) -> list[float]:
    """Select the closest available eta to each requested characteristic value."""
    available = sorted({float(eta) for eta in available_etas})
    selected: list[float] = []
    for requested in requested_etas:
        closest = min(available, key=lambda eta: (abs(eta - requested), eta))
        if closest not in selected:
            selected.append(closest)
    return selected


def _errorbar(ax: plt.Axes, x: pd.Series, y: pd.Series, yerr: pd.Series, **kwargs: object) -> None:
    """Draw error bars while treating a single replica as zero uncertainty."""
    ax.errorbar(
        x.to_numpy(dtype=float),
        y.to_numpy(dtype=float),
        yerr=yerr.fillna(0.0).to_numpy(dtype=float),
        capsize=3,
        linewidth=1.8,
        markersize=5,
        **kwargs,
    )


class TP2FigureGenerator:
    """Load parser-backed telemetry and export the static figures from TP2."""

    def __init__(
        self,
        telemetry_dir: Path,
        output_dir: Path,
        steady_state_fraction: float,
        characteristic_etas: Sequence[float],
        image_format: str,
        dpi: int,
    ) -> None:
        self.telemetry_dir = telemetry_dir
        self.output_dir = output_dir
        self.steady_state_fraction = steady_state_fraction
        self.characteristic_etas = list(characteristic_etas)
        self.image_format = image_format
        self.dpi = dpi
        self.observables = pd.DataFrame()
        self.execution_times = pd.DataFrame()
        self.steady_by_run = pd.DataFrame()
        self.steady_summary = pd.DataFrame()
        self.temporal_summary = pd.DataFrame()
        self.temporal_starts = pd.DataFrame()
        self.created_files: list[Path] = []
        self.warnings: list[str] = []

    def load(self) -> None:
        """Load the CSVs through the existing typed parsers and prepare summaries."""
        observables_path = self.telemetry_dir / "time_observables.csv"
        if not observables_path.exists():
            raise FileNotFoundError(
                f"No se encontró {observables_path}. Se requiere para las figuras b–e."
            )

        observable_parser = TimeObservableParser(observables_path)
        self.observables = _records_to_frame(observable_parser.load())
        if self.observables.empty:
            raise ValueError(f"{observables_path} no contiene observables temporales.")

        self.observables["model"] = _normalise_model(self.observables["model"])
        for column in ("density", "eta", "time", "va", "cluster_ratio"):
            self.observables[column] = pd.to_numeric(self.observables[column], errors="raise")

        # RunConfigParser is loaded as a schema-level validation of the run metadata.
        # The observable's maximum recorded time is used below because it remains
        # correct even when the final simulation step is omitted by the runner.
        config_path = self.telemetry_dir / "run_config.csv"
        if config_path.exists():
            RunConfigParser(config_path).load()
        else:
            self.warnings.append(
                "No se encontró run_config.csv; se infiere el final de cada corrida desde time_observables.csv."
            )

        execution_path = self.telemetry_dir / "execution_times_cim.csv"
        if execution_path.exists():
            execution_parser = ExecutionTimeParser(execution_path)
            self.execution_times = _records_to_frame(execution_parser.load())
            if not self.execution_times.empty:
                self.execution_times["model"] = _normalise_model(self.execution_times["model"])
        else:
            self.warnings.append(
                "No se encontró execution_times_cim.csv; se omitirá la figura de tiempos del CIM."
            )

        self._build_observable_summaries()

    def _build_observable_summaries(self) -> None:
        run_end_time = self.observables.groupby("run_id")["time"].transform("max")
        self.observables = self.observables.assign(
            steady_state_start=run_end_time * self.steady_state_fraction
        )
        condition_columns = ["model", "density", "eta"]
        run_columns = ["run_id", *condition_columns]
        steady_rows = self.observables.loc[
            self.observables["time"] >= self.observables["steady_state_start"]
        ]

        self.steady_by_run = (
            steady_rows.groupby(run_columns, as_index=False)
            .agg(va=("va", "mean"), cluster_ratio=("cluster_ratio", "mean"))
            .sort_values(condition_columns)
        )

        self.steady_summary = (
            self.steady_by_run.groupby(condition_columns, as_index=False)
            .agg(
                va_mean=("va", "mean"),
                va_std=("va", _std_or_zero),
                s_mean=("cluster_ratio", "mean"),
                s_std=("cluster_ratio", _std_or_zero),
                replicas=("run_id", "nunique"),
            )
            .sort_values(condition_columns)
        )

        self.temporal_summary = (
            self.observables.groupby([*condition_columns, "time"], as_index=False)
            .agg(
                va_mean=("va", "mean"),
                va_std=("va", _std_or_zero),
                s_mean=("cluster_ratio", "mean"),
                s_std=("cluster_ratio", _std_or_zero),
                replicas=("run_id", "nunique"),
            )
            .sort_values([*condition_columns, "time"])
        )

        self.temporal_starts = (
            self.observables.groupby(condition_columns, as_index=False)
            .agg(steady_state_start=("steady_state_start", "median"))
            .sort_values(condition_columns)
        )

    def _save(self, figure: plt.Figure, filename: str) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)
        path = self.output_dir / f"{filename}.{self.image_format}"
        figure.savefig(path, dpi=self.dpi, bbox_inches="tight")
        plt.close(figure)
        self.created_files.append(path)

    @staticmethod
    def _set_fraction_axis(ax: plt.Axes, ylabel: str) -> None:
        ax.set_xlabel("Tiempo $t$")
        ax.set_ylabel(ylabel)
        ax.set_ylim(-0.04, 1.04)
        ax.grid(True, alpha=0.28)

    def _temporal_start(self, model: str, density: float, eta: float) -> float | None:
        rows = self.temporal_starts.loc[
            (self.temporal_starts["model"] == model)
            & np.isclose(self.temporal_starts["density"], density)
            & np.isclose(self.temporal_starts["eta"], eta)
        ]
        return None if rows.empty else float(rows.iloc[0]["steady_state_start"])

    def plot_temporal_polarization(self) -> None:
        """Figure b: characteristic Va(t) curves, comparing both models per density."""
        for density in sorted(self.observables["density"].unique()):
            density_data = self.temporal_summary.loc[
                np.isclose(self.temporal_summary["density"], density)
            ]
            etas = _pick_characteristic_etas(
                density_data["eta"].unique(), self.characteristic_etas
            )
            figure, axes = plt.subplots(
                1, len(etas), figsize=(5.2 * len(etas), 4.2), sharey=True, squeeze=False
            )

            for axis, eta in zip(axes.flat, etas):
                panel = density_data.loc[np.isclose(density_data["eta"], eta)]
                starts: list[float] = []
                for model in _ordered_models(panel["model"]):
                    curve = panel.loc[panel["model"] == model]
                    style = _model_style(model)
                    axis.plot(
                        curve["time"],
                        curve["va_mean"],
                        label=_model_label(model),
                        **style,
                    )
                    uncertainty = curve["va_std"].fillna(0.0)
                    if float(uncertainty.max()) > 0.0:
                        axis.fill_between(
                            curve["time"],
                            curve["va_mean"] - uncertainty,
                            curve["va_mean"] + uncertainty,
                            color=style["color"],
                            alpha=0.16,
                        )
                    start = self._temporal_start(model, density, eta)
                    if start is not None:
                        starts.append(start)

                if starts:
                    axis.axvline(
                        float(np.median(starts)),
                        color="black",
                        linestyle=":",
                        linewidth=1.3,
                        label="Inicio estacionario",
                    )
                axis.set_title(f"$\\eta={_number_label(eta)}$")
                self._set_fraction_axis(axis, "Polarización $v_a$")
                axis.legend(fontsize=8)

            figure.suptitle(
                f"Evolución temporal de la polarización ($\\rho={_number_label(density)}$)", y=1.03
            )
            figure.tight_layout()
            self._save(figure, f"temporal_polarizacion_rho_{_safe_density_name(density)}")

    def plot_polarization_vs_eta(self) -> None:
        """Figure c and its requested standard-voter comparison."""
        densities = sorted(self.steady_summary["density"].unique())
        figure, axes = plt.subplots(
            1, len(densities), figsize=(5.2 * len(densities), 4.4), sharey=True, squeeze=False
        )

        for axis, density in zip(axes.flat, densities):
            panel = self.steady_summary.loc[np.isclose(self.steady_summary["density"], density)]
            for model in _ordered_models(panel["model"]):
                curve = panel.loc[panel["model"] == model].sort_values("eta")
                _errorbar(
                    axis,
                    curve["eta"],
                    curve["va_mean"],
                    curve["va_std"],
                    label=_model_label(model),
                    **_model_style(model),
                )
            axis.set_title(f"$\\rho={_number_label(density)}$")
            axis.set_xlabel("Ruido $\\eta$")
            axis.grid(True, alpha=0.28)
            axis.legend(fontsize=8)

        axes.flat[0].set_ylabel("Polarización estacionaria media $\\langle v_a \\rangle$")
        axes.flat[0].set_ylim(-0.04, 1.04)
        figure.suptitle("Polarización estacionaria en función del ruido", y=1.03)
        figure.tight_layout()
        self._save(figure, "polarizacion_vs_eta")

    def plot_temporal_cluster_ratio(self) -> None:
        """First part of figure d: characteristic S(t) curves for each model and density."""
        for model in _ordered_models(self.temporal_summary["model"]):
            model_data = self.temporal_summary.loc[self.temporal_summary["model"] == model]
            for density in sorted(model_data["density"].unique()):
                density_data = model_data.loc[np.isclose(model_data["density"], density)]
                etas = _pick_characteristic_etas(
                    density_data["eta"].unique(), self.characteristic_etas
                )
                figure, axes = plt.subplots(
                    1, len(etas), figsize=(5.2 * len(etas), 4.2), sharey=True, squeeze=False
                )
                style = _model_style(model)

                for axis, eta in zip(axes.flat, etas):
                    curve = density_data.loc[np.isclose(density_data["eta"], eta)]
                    axis.plot(curve["time"], curve["s_mean"], **style)
                    uncertainty = curve["s_std"].fillna(0.0)
                    if float(uncertainty.max()) > 0.0:
                        axis.fill_between(
                            curve["time"],
                            curve["s_mean"] - uncertainty,
                            curve["s_mean"] + uncertainty,
                            color=style["color"],
                            alpha=0.16,
                        )
                    start = self._temporal_start(model, density, eta)
                    if start is not None:
                        axis.axvline(start, color="black", linestyle=":", linewidth=1.3)
                    axis.set_title(f"$\\eta={_number_label(eta)}$")
                    self._set_fraction_axis(axis, "Fracción gigante $S$")

                figure.suptitle(
                    f"Evolución temporal de $S$ - {_model_label(model)} ($\\rho={_number_label(density)}$)",
                    y=1.03,
                )
                figure.tight_layout()
                self._save(
                    figure,
                    f"temporal_cluster_ratio_{model}_rho_{_safe_density_name(density)}",
                )

    def plot_cluster_ratio_vs_eta(self) -> None:
        """Second part of figure d: stationary S(eta), one file for each model."""
        for model in _ordered_models(self.steady_summary["model"]):
            model_data = self.steady_summary.loc[self.steady_summary["model"] == model]
            figure, axis = plt.subplots(figsize=(7.2, 4.8))
            for color_index, density in enumerate(sorted(model_data["density"].unique())):
                curve = model_data.loc[np.isclose(model_data["density"], density)].sort_values("eta")
                _errorbar(
                    axis,
                    curve["eta"],
                    curve["s_mean"],
                    curve["s_std"],
                    label=f"$\\rho={_number_label(density)}$",
                    color=DENSITY_COLORS[color_index % len(DENSITY_COLORS)],
                    marker="o",
                    linestyle="-",
                )
            axis.set_xlabel("Ruido $\\eta$")
            axis.set_ylabel("Fracción estacionaria media $\\langle S \\rangle$")
            axis.set_ylim(-0.04, 1.04)
            axis.grid(True, alpha=0.28)
            axis.legend(title="Densidad", fontsize=9)
            axis.set_title(f"Componente gigante estacionaria - {_model_label(model)}")
            figure.tight_layout()
            self._save(figure, f"cluster_ratio_vs_eta_{model}")

    def plot_polarization_vs_cluster_ratio(self) -> None:
        """Figure e and the required standard-voter comparison."""
        densities = sorted(self.steady_summary["density"].unique())
        figure, axes = plt.subplots(
            1, len(densities), figsize=(5.2 * len(densities), 4.4), sharex=True, sharey=True, squeeze=False
        )

        for axis, density in zip(axes.flat, densities):
            panel = self.steady_summary.loc[np.isclose(self.steady_summary["density"], density)]
            for model in _ordered_models(panel["model"]):
                points = panel.loc[panel["model"] == model].sort_values("eta")
                style = _model_style(model)
                axis.errorbar(
                    points["s_mean"],
                    points["va_mean"],
                    xerr=points["s_std"].fillna(0.0),
                    yerr=points["va_std"].fillna(0.0),
                    label=_model_label(model),
                    capsize=3,
                    linewidth=1.5,
                    markersize=5,
                    **style,
                )
            axis.set_title(f"$\\rho={_number_label(density)}$")
            axis.set_xlabel("Fracción gigante estacionaria $\\langle S \\rangle$")
            axis.grid(True, alpha=0.28)
            axis.legend(fontsize=8)

        axes.flat[0].set_ylabel("Polarización estacionaria $\\langle v_a \\rangle$")
        axes.flat[0].set_xlim(-0.04, 1.04)
        axes.flat[0].set_ylim(-0.04, 1.04)
        figure.suptitle("Polarización en función de la componente gigante", y=1.03)
        figure.tight_layout()
        self._save(figure, "polarizacion_vs_cluster_ratio")

    @staticmethod
    def _tp1_execution_frame(csv_path: Path) -> pd.DataFrame:
        """Accept the common TP1 timing schemas and return N, mean, and standard deviation in ms."""
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
        reported_std_ms: pd.Series
        if std_ms_column is not None:
            reported_std_ms = table[std_ms_column]
        elif std_seconds_column is not None:
            reported_std_ms = table[std_seconds_column] * 1000.0
        else:
            reported_std_ms = pd.Series(np.nan, index=table.index)
        data = pd.DataFrame(
            {
                "n_particles": table[n_column],
                "time_ms": time_ms,
                "reported_std_ms": reported_std_ms,
            }
        )
        data["n_particles"] = pd.to_numeric(data["n_particles"], errors="raise")
        data["time_ms"] = pd.to_numeric(data["time_ms"], errors="raise")
        data["reported_std_ms"] = pd.to_numeric(data["reported_std_ms"], errors="coerce")
        summary = (
            data.groupby("n_particles", as_index=False)
            .agg(
                mean_time_ms=("time_ms", "mean"),
                between_run_std_ms=("time_ms", _std_or_zero),
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

    def plot_execution_times(self, tp1_execution_csv: Path | None) -> None:
        """Figure g: mean CIM step time, optionally contrasted with the TP1 series."""
        if self.execution_times.empty:
            return

        per_run = (
            self.execution_times.groupby(
                ["run_id", "model", "density", "n_particles", "method"], as_index=False
            )
            .agg(time_ms=("execution_time_ms", "mean"))
        )
        summary = (
            per_run.groupby(["model", "density", "n_particles", "method"], as_index=False)
            .agg(
                mean_time_ms=("time_ms", "mean"),
                std_time_ms=("time_ms", _std_or_zero),
                replicas=("run_id", "nunique"),
            )
            .sort_values("n_particles")
        )
        figure, axis = plt.subplots(figsize=(7.6, 5.0))
        plotted_n_particles = set(summary["n_particles"].astype(float))
        for index, ((model, density, method), curve) in enumerate(
            summary.groupby(["model", "density", "method"], sort=True)
        ):
            _errorbar(
                axis,
                curve["n_particles"],
                curve["mean_time_ms"],
                curve["std_time_ms"],
                label=f"TP2 {_model_label(model)}, $\\rho={_number_label(density)}$ ({method})",
                color=DENSITY_COLORS[index % len(DENSITY_COLORS)],
                marker="o",
                linestyle="-",
            )

        if tp1_execution_csv is not None:
            tp1_data = self._tp1_execution_frame(tp1_execution_csv)
            plotted_n_particles.update(tp1_data["n_particles"].astype(float))
            _errorbar(
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
            self.warnings.append(
                "La figura de tiempos muestra solo TP2: use --tp1-execution-csv para agregar la comparación pedida con TP1."
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
        self._save(figure, "tiempos_ejecucion_cim")

    def generate(self, tp1_execution_csv: Path | None) -> list[Path]:
        self.plot_temporal_polarization()
        self.plot_polarization_vs_eta()
        self.plot_temporal_cluster_ratio()
        self.plot_cluster_ratio_vs_eta()
        self.plot_polarization_vs_cluster_ratio()
        self.plot_execution_times(tp1_execution_csv)
        return self.created_files


def _build_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Genera las figuras estáticas requeridas por el enunciado del TP2."
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
        help="CSV de tiempos del TP1 para agregarlo a la figura de tiempos del CIM.",
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

    generator = TP2FigureGenerator(
        telemetry_dir=args.telemetry_dir,
        output_dir=args.output_dir,
        steady_state_fraction=args.steady_state_fraction,
        characteristic_etas=args.characteristic_etas,
        image_format=args.format,
        dpi=args.dpi,
    )
    generator.load()
    created_files = generator.generate(args.tp1_execution_csv)

    for warning in generator.warnings:
        print(f"Advertencia: {warning}")
    print(f"Se generaron {len(created_files)} figuras en {args.output_dir}:")
    for path in created_files:
        print(f"- {path.name}")


if __name__ == "__main__":
    main()
