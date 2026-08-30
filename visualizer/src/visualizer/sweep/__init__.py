"""Static TP2 figures, one small module per figure.

Every module here does exactly one plot and exposes a single
``generate(bundle, output_dir, ...) -> list[Path]`` function. They all share
the telemetry loaded once by :mod:`visualizer.sweep.telemetry` and the small
helpers in :mod:`visualizer.sweep.common`.

Run ``python3 -m visualizer.sweep`` to generate every figure at once (see
``__main__.py``), or import an individual ``plot_*`` module to regenerate a
single figure.
"""

from .telemetry import TelemetryBundle, load_telemetry
from . import (
    plot_cluster_ratio_vs_eta,
    plot_execution_times,
    plot_polarization_vs_cluster_ratio,
    plot_polarization_vs_eta,
    plot_temporal_cluster_ratio,
    plot_temporal_polarization,
)

__all__ = [
    "TelemetryBundle",
    "load_telemetry",
    "plot_temporal_polarization",
    "plot_polarization_vs_eta",
    "plot_temporal_cluster_ratio",
    "plot_cluster_ratio_vs_eta",
    "plot_polarization_vs_cluster_ratio",
    "plot_execution_times",
]
