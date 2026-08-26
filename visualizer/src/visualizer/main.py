import re
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.patches import Circle

# File paths
DYNAMIC_DATA_PATH = "out/dynamic_data.csv"
STATIC_DATA_PATH = "out/static_data.csv"

# Color definitions
COLOR_DEFAULT = "#A0A0A0"  # Light grey
COLOR_CLICKED = "#E74C3C"  # Red
COLOR_NEIGHBOUR = "#3498DB"  # Blue
COLOR_EDGE = "#2C3E50"  # Dark border


def parse_neighbours(val):
    """Parses neighbour values into a list of integer IDs (handles spaces, commas, brackets)."""
    if pd.isna(val) or val is None:
        return []
    val_str = str(val).strip("[]() \t\n\r")
    if not val_str:
        return []
    tokens = re.split(r"[\s,]+", val_str)
    neighbours = []
    for token in tokens:
        if token:
            try:
                neighbours.append(int(float(token)))
            except ValueError:
                pass
    return neighbours


def main():
    # 1. Load data
    dynamic_df = pd.read_csv(DYNAMIC_DATA_PATH)
    static_df = pd.read_csv(STATIC_DATA_PATH)

    # Filter for t = 0
    t0_df = dynamic_df[dynamic_df["t"] == 0].copy()

    # Merge dynamic and static data on 'ID'
    merged_df = t0_df.merge(static_df, on="ID")

    # Parse neighbours column into lists of IDs
    merged_df["neighbours_list"] = merged_df["neighbours"].apply(
        parse_neighbours
    )

    # 2. Setup Plot
    fig, ax = plt.subplots(figsize=(8, 8))
    ax.set_aspect("equal")
    ax.set_title(
        "Particle System (t=0)\nClick on a particle to highlight its neighbours",
        fontsize=12,
    )
    ax.set_xlabel("X Position")
    ax.set_ylabel("Y Position")

    # Dictionary mapping particle ID -> Circle patch & particle data
    particle_patches = {}
    particle_data = {}

    for _, row in merged_df.iterrows():
        p_id = int(row["ID"])
        x, y = row["Xpos"], row["Ypos"]
        r = row["radius"]
        neighbours = row["neighbours_list"]

        # Create Circle patch
        circle = Circle(
            (x, y),
            radius=r,
            facecolor=COLOR_DEFAULT,
            edgecolor=COLOR_EDGE,
            linewidth=1.0,
            zorder=2,
        )
        ax.add_patch(circle)

        # Store particle metadata and patch
        particle_patches[p_id] = circle
        particle_data[p_id] = {
            "x": x,
            "y": y,
            "radius": r,
            "neighbours": neighbours,
        }

    # Auto-scale plot limits with padding
    ax.autoscale_view()
    ax.margins(0.05)

    # 3. Interactive Click Event Handler
    def on_click(event):
        if event.xdata is None or event.ydata is None:
            return  # Click was outside the axes

        click_x, click_y = event.xdata, event.ydata
        clicked_id = None
        min_dist = float("inf")

        # Find which particle was clicked (closest center within radius)
        for p_id, data in particle_data.items():
            dist = (
                (click_x - data["x"]) ** 2 + (click_y - data["y"]) ** 2
            ) ** 0.5
            if dist <= data["radius"] and dist < min_dist:
                min_dist = dist
                clicked_id = p_id

        # Reset all particles to default color
        for circle in particle_patches.values():
            circle.set_facecolor(COLOR_DEFAULT)

        # Update highlighted colors if a particle was selected
        if clicked_id is not None:
            neighbours = particle_data[clicked_id]["neighbours"]

            # Color neighbours blue
            for n_id in neighbours:
                if n_id in particle_patches:
                    particle_patches[n_id].set_facecolor(COLOR_NEIGHBOUR)

            # Color clicked particle red
            particle_patches[clicked_id].set_facecolor(COLOR_CLICKED)

            ax.set_title(
                f"Selected Particle ID: {clicked_id} (Red) | Neighbours: {len(neighbours)} (Blue)",
                fontsize=12,
            )
        else:
            ax.set_title(
                "Particle System (t=0)\nClick on a particle to highlight its neighbours",
                fontsize=12,
            )

        fig.canvas.draw_idle()

    # Connect click event
    fig.canvas.mpl_connect("button_press_event", on_click)

    plt.show()


if __name__ == "__main__":
    main()