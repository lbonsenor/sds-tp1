"""Figure (b): characteristic evolución temporal de la polarización.

One file per density, with a panel per characteristic eta, comparing the
Estándar and Votante models in the same axes and marking the median start of
the steady-state window used for figures (c) and (e).
"""

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
    for density in sorted(bundle.observables["density"].unique()):
        density_data = bundle.temporal_summary.loc[
            np.isclose(bundle.temporal_summary["density"], density)
        ]
        etas = pick_characteristic_etas(
            [float(eta) for eta in density_data["eta"].unique()], characteristic_etas
        )
        figure, axes = plt.subplots(
            1, len(etas), figsize=(5.2 * len(etas), 4.2), sharey=True, squeeze=False
        )

        for axis, eta in zip(axes.flat, etas):
            panel = density_data.loc[np.isclose(density_data["eta"], eta)]
            starts: list[float] = []
            for model in ordered_models(panel["model"]):
                curve = panel.loc[panel["model"] == model]
                style = model_style(model)
                axis.plot(curve["time"], curve["va_mean"], label=model_label(model), **style)
                uncertainty = curve["va_std"].fillna(0.0)
                if float(uncertainty.max()) > 0.0:
                    axis.fill_between(
                        curve["time"],
                        curve["va_mean"] - uncertainty,
                        curve["va_mean"] + uncertainty,
                        color=style["color"],
                        alpha=0.16,
                    )
                start = bundle.temporal_start(model, density, eta)
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
            axis.set_title(f"$\\eta={number_label(eta)}$")
            set_fraction_axis(axis, "Polarización va")
            axis.legend(fontsize=8)

        figure.suptitle(
            f"Evolución temporal de la polarización ($\\rho={number_label(density)}$)", y=1.03
        )
        figure.tight_layout()
        created.append(
            save_figure(
                figure,
                output_dir,
                f"temporal_polarizacion_rho_{safe_density_name(density)}",
                image_format,
                dpi,
            )
        )
    return created