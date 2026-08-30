"""Figure (d), first half: evolución temporal de la fracción del cluster gigante S.

One file per (model, density), with a panel per characteristic eta -- mirrors
``plot_temporal_polarization`` but for the clustering observable.
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
    for model in ordered_models(bundle.temporal_summary["model"]):
        model_data = bundle.temporal_summary.loc[bundle.temporal_summary["model"] == model]
        for density in sorted(model_data["density"].unique()):
            density_data = model_data.loc[np.isclose(model_data["density"], density)]
            etas = pick_characteristic_etas(
                [float(eta) for eta in density_data["eta"].unique()], characteristic_etas
            )
            figure, axes = plt.subplots(
                1, len(etas), figsize=(5.2 * len(etas), 4.2), sharey=True, squeeze=False
            )
            style = model_style(model)

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
                start = bundle.temporal_start(model, density, eta)
                if start is not None:
                    axis.axvline(start, color="black", linestyle=":", linewidth=1.3)
                axis.set_title(f"$\\eta={number_label(eta)}$")
                set_fraction_axis(axis, "Fracción gigante $S$")

            figure.suptitle(
                f"Evolución temporal de $S$ - {model_label(model)} ($\\rho={number_label(density)}$)",
                y=1.03,
            )
            figure.tight_layout()
            created.append(
                save_figure(
                    figure,
                    output_dir,
                    f"temporal_cluster_ratio_{model}_rho_{safe_density_name(density)}",
                    image_format,
                    dpi,
                )
            )
    return created