"""Figure (d), second half, and its requested Estándar-Votante comparison (f)."""

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
        # Filtrar por densidad (usando tolerancia para evitar problemas con números flotantes)
        panel = bundle.steady_summary.loc[np.isclose(bundle.steady_summary["density"], density)]
        
        for model in ordered_models(panel["model"]):
            # Filtrar por modelo y asegurar un ordenamiento numérico estricto por eta
            curve = (
                panel.loc[panel["model"] == model]
                .assign(eta=lambda df: df["eta"].round(6)) # Evita duplicados o desorden por precisión flotante
                .sort_values("eta")
                .drop_duplicates(subset=["eta"])
            )
            
            # El enunciado solicita explícitamente "el valor medio de S en el estacionario con su desvío"
            # Por lo tanto, se utiliza directamente s_std (desviación estándar) y no el error estándar (SE).
            s_error = curve["s_std"]
            
            errorbar(
                axis,
                curve["eta"],
                curve["s_mean"],
                s_error,
                label=model_label(model),
                **model_style(model),
            )
        
        axis.set_title(f"$\\rho={number_label(density)}$")
        axis.set_xlabel("Ruido eta")
        axis.legend(fontsize=8)

    axes.flat[0].set_ylabel("Fracción gigante estacionaria media S")
    axes.flat[0].set_ylim(-0.04, 1.04)
    # figure.suptitle("Componente gigante estacionaria en función del ruido", y=1.03)
    figure.tight_layout()
    
    return [save_figure(figure, output_dir, "cluster_ratio_vs_eta", image_format, dpi)]