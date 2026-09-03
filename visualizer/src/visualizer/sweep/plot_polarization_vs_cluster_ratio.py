"""Figure (e) and its requested Estándar-Votante comparison (f): va vs S."""

from __future__ import annotations

from pathlib import Path
from typing import Iterable, Sequence

import matplotlib.pyplot as plt
import numpy as np

from .common import model_label, model_style, number_label, ordered_models, save_figure
from .telemetry import TelemetryBundle


def _safe_model_name(model: str) -> str:
    return str(model).strip().lower().replace(" ", "_")


def _plot_models(axis, panel, models: Iterable[str]) -> None:
    for model in models:
        points = panel.loc[panel["model"] == model].sort_values("eta")
        style = model_style(model)

        # Error Estándar de la Media para ambos ejes
        n_replicas = np.maximum(points["replicas"].to_numpy(), 1)
        s_se = points["s_std"].fillna(0.0).to_numpy() / np.sqrt(n_replicas)
        va_se = points["va_std"].fillna(0.0).to_numpy() / np.sqrt(n_replicas)

        axis.errorbar(
            points["s_mean"],
            points["va_mean"],
            xerr=s_se,
            yerr=va_se,
            label=model_label(model),
            capsize=3,
            linewidth=1.5,
            markersize=5,
            **style,
        )


def _build_figure(bundle: TelemetryBundle, densities: Sequence[float], models: Sequence[str]):
    figure, axes = plt.subplots(
        1, len(densities), figsize=(5.2 * len(densities), 4.4), sharex=True, sharey=True, squeeze=False
    )

    for axis, density in zip(axes.flat, densities):
        panel = bundle.steady_summary.loc[np.isclose(bundle.steady_summary["density"], density)]
        _plot_models(axis, panel, models)
        axis.set_title(f"$\\rho={number_label(density)}$")
        axis.set_xlabel("Fracción gigante estacionaria S")
        axis.legend(fontsize=8)

    axes.flat[0].set_ylabel("Polarización estacionaria va")
    axes.flat[0].set_xlim(-0.04, 1.04)
    axes.flat[0].set_ylim(-0.04, 1.04)
    figure.tight_layout()
    return figure


def generate(bundle: TelemetryBundle, output_dir: Path, image_format: str, dpi: int) -> list[Path]:
    # Filtrar densidades excluyendo rho=0.4 y rho=0.16
    raw_densities = bundle.steady_summary["density"].unique()
    densities = sorted(
        d for d in raw_densities
        if not (np.isclose(d, 0.4) or np.isclose(d, 0.16))
    )

    all_models = ordered_models(bundle.steady_summary["model"])

    created: list[Path] = []

    # Gráfico combinado (Estándar y Votante superpuestos) — comportamiento original, preservado.
    combined_figure = _build_figure(bundle, densities, all_models)
    created.append(
        save_figure(combined_figure, output_dir, "polarizacion_vs_cluster_ratio", image_format, dpi)
    )

    # Un gráfico adicional por modelo, aislado.
    for model in all_models:
        model_figure = _build_figure(bundle, densities, [model])
        created.append(
            save_figure(
                model_figure,
                output_dir,
                f"polarizacion_vs_cluster_ratio_{_safe_model_name(model)}",
                image_format,
                dpi,
            )
        )

    return created