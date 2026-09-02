from __future__ import annotations

from pathlib import Path
from typing import Sequence

import matplotlib.pyplot as plt
import numpy as np

from .common import (
    model_label,
    model_style,
    number_label,
    ordered_models,
    pick_characteristic_etas,
    safe_density_name,
    save_figure,
    set_fraction_axis,
)
from .telemetry import TelemetryBundle


def generate(
    bundle: TelemetryBundle,
    output_dir: Path,
    characteristic_etas: Sequence[float],
    image_format: str,
    dpi: int,
) -> list[Path]:
    created: list[Path] = []
    
    # 1. Obtener los modelos ordenados y las densidades comunes disponibles
    models = ordered_models(bundle.temporal_summary["model"])
    densities = sorted(bundle.temporal_summary["density"].unique())

    # 2. Iterar por densidad para superponer todos los modelos en un mismo gráfico
    for density in densities:
        density_data = bundle.temporal_summary.loc[
            np.isclose(bundle.temporal_summary["density"], density)
        ]
        
        # Seleccionar etas representativos disponibles para esta densidad
        etas = pick_characteristic_etas(
            [float(eta) for eta in density_data["eta"].unique()], characteristic_etas
        )
        
        figure, axes = plt.subplots(
            1, len(etas), figsize=(5.2 * len(etas), 4.2), sharey=True, squeeze=False
        )

        for axis, eta in zip(axes.flat, etas):
            eta_data = density_data.loc[np.isclose(density_data["eta"], eta)]
            
            # Graficar cada modelo dentro del subplot correspondiente a η
            for model in models:
                curve = eta_data.loc[eta_data["model"] == model]
                if curve.empty:
                    continue
                
                style = model_style(model)
                label = model_label(model)
                
                # Curva temporal del modelo
                axis.plot(curve["time"], curve["s_mean"], label=label, **style)
                
                # Intervalo de incertidumbre / desvío
                uncertainty = curve["s_std"].fillna(0.0)
                if float(uncertainty.max()) > 0.0:
                    axis.fill_between(
                        curve["time"],
                        curve["s_mean"] - uncertainty,
                        curve["s_mean"] + uncertainty,
                        color=style["color"],
                        alpha=0.16,
                    )
                
                # Línea vertical del inicio del estado estacionario
                start = bundle.temporal_start(model, density, eta)
                if start is not None:
                    axis.axvline(
                        start, 
                        color=style["color"], 
                        linestyle=":", 
                        linewidth=1.3,
                        alpha=0.85
                    )

            axis.set_title(f"$\\eta={number_label(eta)}$")
            set_fraction_axis(axis, "Fracción gigante S")
            axis.legend(loc="best")

        figure.suptitle(
            f"Evolución temporal de $S$ ($\\rho={number_label(density)}$)",
            y=1.03,
        )
        figure.tight_layout()
        
        created.append(
            save_figure(
                figure,
                output_dir,
                f"temporal_cluster_ratio_rho_{safe_density_name(density)}",
                image_format,
                dpi,
            )
        )
        
    return created