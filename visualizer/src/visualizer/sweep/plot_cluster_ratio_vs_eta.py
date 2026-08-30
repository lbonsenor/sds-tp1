"""Figure (d), second half, and its requested Estándar-Votante comparison (f).

FIX vs. the original TP2 script: ``plot_cluster_ratio_vs_eta`` used to save
one *separate* file per model (``cluster_ratio_vs_eta_standard.png`` and
``..._voter.png``), so the comparison requested by point (f) of the
enunciado never actually happened for this observable, unlike
``polarizacion_vs_eta`` which always overlaid both models. This module now
mirrors ``plot_polarization_vs_eta`` exactly: one panel per density, both
models overlaid in each panel, in a single ``cluster_ratio_vs_eta.png``.
"""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

from .common import errorbar, model_label, model_style, number_label, ordered_models, save_figure
from .telemetry import TelemetryBundle


def generate(bundle: TelemetryBundle, output_dir: Path, image_format: str, dpi: int) -> list[Path]:
    densities = sorted(bundle.steady_summary["density"].unique())
    figure, axes = plt.subplots(
        1, len(densities), figsize=(5.2 * len(densities), 4.4), sharey=True, squeeze=False
    )

    for axis, density in zip(axes.flat, densities):
        panel = bundle.steady_summary.loc[np.isclose(bundle.steady_summary["density"], density)]
        for model in ordered_models(panel["model"]):
            curve = panel.loc[panel["model"] == model].sort_values("eta")
            errorbar(
                axis,
                curve["eta"],
                curve["s_mean"],
                curve["s_std"],
                label=model_label(model),
                **model_style(model),
            )
        axis.set_title(f"$\\rho={number_label(density)}$")
        axis.set_xlabel("Ruido $\\eta$")
        axis.grid(True, alpha=0.28)
        axis.legend(fontsize=8)

    axes.flat[0].set_ylabel("Fracción gigante estacionaria media $\\langle S \\rangle$")
    axes.flat[0].set_ylim(-0.04, 1.04)
    figure.suptitle("Componente gigante estacionaria en función del ruido", y=1.03)
    figure.tight_layout()
    return [save_figure(figure, output_dir, "cluster_ratio_vs_eta", image_format, dpi)]
