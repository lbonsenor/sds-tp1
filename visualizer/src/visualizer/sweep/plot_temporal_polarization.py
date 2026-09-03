"""Figure (b): characteristic evolución temporal de la polarización.

One file per density, with a panel per characteristic eta, comparing the
Estándar and Votante models in the same axes. Also produces, per density,
one additional isolated figure per model.
"""

from __future__ import annotations

from pathlib import Path
from typing import Iterable, Sequence

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


def _safe_model_name(model: str) -> str:
    return str(model).strip().lower().replace(" ", "_")


def _plot_models(axis, panel, models: Iterable[str]) -> None:
    for model in models:
        curve = panel.loc[panel["model"] == model]
        if curve.empty:
            continue

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


def _build_figure(density_data, etas: Sequence[float], models: Sequence[str]):
    figure, axes = plt.subplots(
        1, len(etas), figsize=(5.2 * len(etas), 4.2), sharey=True, squeeze=False
    )

    for axis, eta in zip(axes.flat, etas):
        panel = density_data.loc[np.isclose(density_data["eta"], eta)]
        _plot_models(axis, panel, models)
        axis.set_title(f"$\\eta={number_label(eta)}$")
        set_fraction_axis(axis, "Polarización va")
        axis.legend(fontsize=8)

    figure.tight_layout()
    return figure


def generate(
    bundle: TelemetryBundle,
    output_dir: Path,
    characteristic_etas: Sequence[float],
    image_format: str,
    dpi: int,
) -> list[Path]:
    created: list[Path] = []

    models = ordered_models(bundle.temporal_summary["model"])

    for density in sorted(bundle.observables["density"].unique()):
        density_data = bundle.temporal_summary.loc[
            np.isclose(bundle.temporal_summary["density"], density)
        ]
        etas = pick_characteristic_etas(
            [float(eta) for eta in density_data["eta"].unique()], characteristic_etas
        )

        # Gráfico combinado (Estándar y Votante superpuestos) — comportamiento original, preservado.
        combined_figure = _build_figure(density_data, etas, models)
        created.append(
            save_figure(
                combined_figure,
                output_dir,
                f"temporal_polarizacion_rho_{safe_density_name(density)}",
                image_format,
                dpi,
            )
        )

        # Un gráfico adicional por modelo, aislado, para esta misma densidad.
        for model in models:
            model_figure = _build_figure(density_data, etas, [model])
            created.append(
                save_figure(
                    model_figure,
                    output_dir,
                    f"temporal_polarizacion_rho_{safe_density_name(density)}_{_safe_model_name(model)}",
                    image_format,
                    dpi,
                )
            )

    return created