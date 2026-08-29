from typing import Any, Dict, List, Optional
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.animation as animation
from matplotlib.artist import Artist
from matplotlib.colors import Normalize
from visualizer.parser import ParticleDataRecord, RunConfigRecord


def animate_particles(
    particle_records: List[ParticleDataRecord],
    config: Optional[RunConfigRecord] = None,
    show_vectors: bool = True,
    vector_scale: float = 0.4,
    interval: int = 40,
) -> animation.FuncAnimation:
    """Animates particle trajectory telemetry for a specific simulation run_id using Matplotlib.

    Handles periodic boundaries and heading vector visualization with blitted updates.
    """
    if not particle_records:
        raise ValueError("No particle records provided for animation.")

    # 1. Extract dynamic metadata from parsed RunConfigRecord if provided
    run_id = particle_records[0].run_id
    box_length = float(config.length) if config else 10.0
    is_periodic = config.periodic_boundary if config else True

    # Convert dataclass records to DataFrame
    df = pd.DataFrame([vars(r) for r in particle_records])

    # 2. Handle Periodic Boundary Wraparound
    if is_periodic:
        df["x"] = df["x"] % box_length
        df["y"] = df["y"] % box_length

    # Calculate velocity vectors
    df["u"] = np.cos(df["theta"]) * vector_scale
    df["v"] = np.sin(df["theta"]) * vector_scale

    timesteps = sorted(df["time"].unique())

    # 3. Setup Figure, Axes, and Color Norm
    fig, ax = plt.subplots(figsize=(7.5, 7.5))
    ax.set_xlim(0, box_length)
    ax.set_ylim(0, box_length)
    ax.set_aspect("equal")
    ax.set_xlabel("X Position")
    ax.set_ylabel("Y Position")
    ax.set_title(f"Particle Flocking Dynamics | Run: {run_id}")

    norm = Normalize(vmin=-np.pi, vmax=np.pi)
    cmap = plt.get_cmap("hsv")

    # 4. Initialize Base Artists using Frame 0 Data
    t0_df = df[df["time"] == timesteps[0]]

    scat = ax.scatter(
        t0_df["x"],
        t0_df["y"],
        c=t0_df["theta"],
        cmap=cmap,
        norm=norm,
        edgecolors="none",
        zorder=3,
    )
    cbar = fig.colorbar(scat, ax=ax, shrink=0.8)
    cbar.set_label("Angle (rad)")

    quiver = None
    if show_vectors:
        quiver = ax.quiver(
            t0_df["x"],
            t0_df["y"],
            t0_df["u"],
            t0_df["v"],
            scale_units="xy",
            scale=1,
            pivot="tail",  # Anchors vector base at particle location
            color=(0.35, 0.35, 0.35, 0.7),
            width=0.003,
            zorder=2,
        )

    # 5. Update Routine for FuncAnimation (Blitted)
    def update(frame_time: Any) -> List[Artist]:
        time_df = df[df["time"] == frame_time]
        offsets = np.c_[time_df["x"].values, time_df["y"].values]

        scat.set_offsets(offsets)
        scat.set_array(time_df["theta"].values) # Will now map properly within [-pi, pi]

        artists: List[Artist] = [scat]

        if show_vectors and quiver is not None:
            quiver.set_offsets(offsets)
            quiver.set_UVC(time_df["u"].values, time_df["v"].values)
            artists.append(quiver)

        return artists

    anim = animation.FuncAnimation(
        fig=fig,
        func=update,
        frames=timesteps,
        interval=interval,
        blit=True,
        repeat=True,
    )

    return anim