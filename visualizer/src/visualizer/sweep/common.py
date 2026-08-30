"""Small, dependency-free helpers shared by every ``sweep`` plot module.

Nothing here talks to the filesystem or knows about a specific figure; it is
just the vocabulary (colors, labels, dataframe plumbing) that every plot
module reuses so that all the TP2 figures look consistent with each other.
"""

from __future__ import annotations

from dataclasses import asdict
from pathlib import Path
from typing import Any, Iterable, Sequence

import matplotlib.axes as maxes
import matplotlib.figure as mfigure
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

MODEL_LABELS = {"standard": "Estándar", "voter": "Votante"}
MODEL_STYLES = {
    "standard": {"color": "#1f77b4", "marker": "o", "linestyle": "-"},
    "voter": {"color": "#d62728", "marker": "s", "linestyle": "--"},
}
DENSITY_COLORS = ["#1f77b4", "#ff7f0e", "#2ca02c", "#9467bd", "#8c564b"]


def records_to_frame(records: Iterable[Any]) -> pd.DataFrame:
    """Build a DataFrame from the typed records returned by the visualizer parsers."""
    return pd.DataFrame(asdict(record) for record in records)


def normalise_model(values: pd.Series) -> pd.Series:
    """Keep model names stable despite the Java exporter using mixed case/language."""
    return values.astype(str).str.strip().str.lower().replace(
        {
            "vicsek": "standard",
            "standard": "standard",
            "voter": "voter",
            "votante": "voter",
        }
    )


def number_label(value: float) -> str:
    return f"{float(value):g}"


def safe_density_name(density: float) -> str:
    """Build a readable, filesystem-safe density fragment for a figure name."""
    return number_label(density).replace("-", "neg").replace(".", "_")


def ordered_models(values: Iterable[str]) -> list[str]:
    """Standard before voter, then anything else alphabetically."""
    models = sorted(set(values))
    return sorted(models, key=lambda model: (model not in MODEL_LABELS, model))


def model_label(model: str) -> str:
    return MODEL_LABELS.get(model, model.replace("_", " ").title())


def model_style(model: str) -> dict[str, str]:
    if model in MODEL_STYLES:
        return MODEL_STYLES[model]
    fallback_index = sum(ord(character) for character in model) % len(DENSITY_COLORS)
    return {"color": DENSITY_COLORS[fallback_index], "marker": "^", "linestyle": "-"}


def std_or_zero(values: pd.Series) -> float:
    std = float(values.std(ddof=1)) if len(values) > 1 else 0.0
    return 0.0 if np.isnan(std) else std


def pick_characteristic_etas(
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


def errorbar(ax: maxes.Axes, x: pd.Series, y: pd.Series, yerr: pd.Series, **kwargs: Any) -> None:
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


def set_fraction_axis(ax: maxes.Axes, ylabel: str) -> None:
    ax.set_xlabel("Tiempo t (s)")
    ax.set_ylabel(ylabel)
    ax.set_ylim(-0.04, 1.04)
    # ax.grid(True, alpha=0.28)


def save_figure(
    figure: mfigure.Figure, output_dir: Path, filename: str, image_format: str, dpi: int
) -> Path:
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / f"{filename}.{image_format}"
    figure.savefig(path, dpi=dpi, bbox_inches="tight")
    plt.close(figure)
    return path