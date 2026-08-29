from pathlib import Path
import matplotlib.pyplot as plt
from visualizer.animate import animate_particles
from visualizer.parser import ParticleDataParser, RunConfigParser

ROOT_DIR = Path(__file__).resolve().parents[3]
TELEMETRY_DIR = ROOT_DIR / "telemetry"


def animate_run(target_run_identifier: str) -> None:
    """Animates telemetry filtered by a full run_id or a prefix (e.g. 'anim_std_high')."""
    config_parser = RunConfigParser(TELEMETRY_DIR / "run_config.csv")
    config_parser.load()

    particle_parser = ParticleDataParser(TELEMETRY_DIR / "particle_data.csv")
    all_particle_records = particle_parser.load()

    # Filter Particle Records by exact run_id or run_prefix
    filtered_records = [
        r
        for r in all_particle_records
        if r.run_id == target_run_identifier or r.run_id.startswith(f"{target_run_identifier}_")
    ]

    if not filtered_records:
        available_ids = set(r.run_id for r in all_particle_records)
        raise ValueError(f"No telemetry found for '{target_run_identifier}'. Available runs: {available_ids}")

    actual_run_id = filtered_records[0].run_id
    config = config_parser.get_config_by_run_id(actual_run_id)

    # Reassigned to `anim` so Python doesn't garbage-collect the animation reference before rendering
    anim = animate_particles(filtered_records, config=config)
    
    # Render the Matplotlib figure window
    anim.save("particle_animation.mp4", writer="ffmpeg", fps=25)
    print("Animation saved successfully.")


if __name__ == "__main__":
    animate_run("anim_vot_low")