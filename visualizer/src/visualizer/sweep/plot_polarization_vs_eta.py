"""Figure (c) and its requested Estándar-Votante comparison (f)."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

from .common import errorbar, model_label, model_style, number_label, ordered_models, save_figure
from .telemetry import TelemetryBundle


def generate(bundle: TelemetryBundle, output_dir: Path, image_format: str, dpi: int) -> list[Path]:
    # Filtrar densidades excluyendo rho=0.4 y rho=0.16
    raw_densities = bundle.steady_summary["density"].unique()
    densities = sorted(
        d for d in raw_densities 
        if not (np.isclose(d, 0.4) or np.isclose(d, 0.16))
    )

    figure, axes = plt.subplots(
        1, len(densities), figsize=(5.2 * len(densities), 4.4), sharey=True, squeeze=False
    )

    for axis, density in zip(axes.flat, densities):
        panel = bundle.steady_summary.loc[np.isclose(bundle.steady_summary["density"], density)]
        for model in ordered_models(panel["model"]):
            curve = panel.loc[panel["model"] == model].sort_values("eta")
            
            # Error Estándar de la Media (SE) manteniendo el tipo pandas.Series
            n_replicas = curve["replicas"].clip(lower=1)
            va_se = curve["va_std"] / np.sqrt(n_replicas)
            
            errorbar(
                axis,
                curve["eta"],
                curve["va_mean"],
                va_se,
                label=model_label(model),
                **model_style(model),
            )
        axis.set_title(f"$\\rho={number_label(density)}$")
        axis.set_xlabel("Ruido eta")
        axis.legend(fontsize=8)

    axes.flat[0].set_ylabel("Polarización estacionaria media va")
    axes.flat[0].set_ylim(-0.04, 1.04)
    # figure.suptitle("Polarización estacionaria en función del ruido", y=1.03)
    figure.tight_layout()
    return [save_figure(figure, output_dir, "polarizacion_vs_eta", image_format, dpi)]