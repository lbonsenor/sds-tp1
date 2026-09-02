"""Figure (e) and its requested Estándar-Votante comparison (f): va vs S."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

from .common import model_label, model_style, number_label, ordered_models, save_figure
from .telemetry import TelemetryBundle


def generate(bundle: TelemetryBundle, output_dir: Path, image_format: str, dpi: int) -> list[Path]:
    # Filtrar densidades excluyendo rho=0.4 y rho=0.16
    raw_densities = bundle.steady_summary["density"].unique()
    densities = sorted(
        d for d in raw_densities 
        if not (np.isclose(d, 0.4) or np.isclose(d, 0.16))
    )

    figure, axes = plt.subplots(
        1, len(densities), figsize=(5.2 * len(densities), 4.4), sharex=True, sharey=True, squeeze=False
    )

    for axis, density in zip(axes.flat, densities):
        panel = bundle.steady_summary.loc[np.isclose(bundle.steady_summary["density"], density)]
        for model in ordered_models(panel["model"]):
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
        axis.set_title(f"$\\rho={number_label(density)}$")
        axis.set_xlabel("Fracción gigante estacionaria S")
        axis.legend(fontsize=8)

    axes.flat[0].set_ylabel("Polarización estacionaria va")
    axes.flat[0].set_xlim(-0.04, 1.04)
    axes.flat[0].set_ylim(-0.04, 1.04)
    # figure.suptitle("Polarización en función de la componente gigante", y=1.03)
    figure.tight_layout()
    return [save_figure(figure, output_dir, "polarizacion_vs_cluster_ratio", image_format, dpi)]