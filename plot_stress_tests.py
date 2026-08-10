import os
import glob
import re
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Set overall seaborn aesthetic style
sns.set_theme(style="whitegrid", palette="deep")

# Ensure output directory exists
output_dir = "figures"
telemetry_dir = "telemetry"
os.makedirs(output_dir, exist_ok=True)

print("Starting plot generation...")

# ====================================================================
# Helper: render the fixed execution parameters as an annotation box
# ====================================================================
ALL_PARAM_COLS = ["N", "L", "M", "rc", "riMin", "riMax", "density",
                  "contour", "warmup_runs", "benchmark_iterations"]

PARAM_LABELS = {
    "N": "N",
    "L": "L",
    "M": "M",
    "rc": "rc",
    "density": "density",
    "contour": "contour",
    "warmup_runs": "warmup",
    "benchmark_iterations": "iters",
}


def format_param_value(col, val):
    if col == "contour":
        return "periodic" if int(val) else "walls"
    return f"{val:g}"


def params_text(df, exclude=()):
    """Builds a compact string with every fixed execution parameter present in the CSV."""
    row = df.iloc[0]
    parts = []
    if {"riMin", "riMax"} <= set(df.columns):
        parts.append(f"ri\u2208[{row['riMin']:g},{row['riMax']:g}]")
    for col in ALL_PARAM_COLS:
        if col in exclude or col in ("riMin", "riMax"):
            continue
        if col in df.columns:
            parts.append(f"{PARAM_LABELS[col]}={format_param_value(col, row[col])}")
    return " | ".join(parts)


def add_params_box(ax, text, x=0.03, y=0.03, ha="left"):
    ax.text(x, y, text, transform=ax.transAxes, fontsize=8, va="bottom", ha=ha,
            bbox=dict(boxstyle="round,pad=0.4", facecolor="white", edgecolor="gray", alpha=0.85))


# ====================================================================
# 1. Process and Plot Variation of M (Section 3)
# ====================================================================
m_csv_files = glob.glob(os.path.join(telemetry_dir, "variation_m_N*.csv"))

if not m_csv_files:
    print(f"No Variation M CSV files found in {telemetry_dir}/")

for path in sorted(m_csv_files):
    filename = os.path.splitext(os.path.basename(path))[0]
    df = pd.read_csv(path)

    # Extract the N value from the filename
    m_match = re.search(r"variation_m_N(\d+)", filename, re.IGNORECASE)
    if m_match:
        n_val = m_match.group(1)
        title = f"Execution Time vs M (N={n_val})"

        plt.figure(figsize=(8, 6))

        # Plot with explicit error bars using standard deviation
        plt.errorbar(
            df["M"],
            df["mean_time_ms"],
            yerr=df["std_dev_ms"],
            fmt='-o',
            capsize=5,  # Width of the error bar caps
            capthick=1.5,  # Thickness of the caps
            color=sns.color_palette()[0]
        )

        plt.title(title, fontsize=14, fontweight="bold")
        plt.xlabel("M (Grid Divisions)")
        plt.ylabel("Mean Execution Time (ms)")

        # Use log scale in case times vary by orders of magnitude (e.g., M=1 vs Optimal)
        plt.yscale("log")

        # Fixed execution parameters (everything except the varying M)
        add_params_box(plt.gca(), params_text(df, exclude=("M",)))
        print(f"  [{filename}] Fixed params: {params_text(df, exclude=('M',))}")

        plt.tight_layout()
        save_path = os.path.join(output_dir, f"{filename}.png")
        plt.savefig(save_path, dpi=300)
        plt.close()

        print(f"Saved figure: {save_path}")

# ====================================================================
# 2. Process and Plot Variation of N (Sections 4.1 & 4.2 Superimposed)
# ====================================================================
free_path = os.path.join(telemetry_dir, "variation_n_free_density.csv")
fixed_path = os.path.join(telemetry_dir, "variation_n_fixed_density.csv")

if os.path.exists(free_path) and os.path.exists(fixed_path):
    df_free = pd.read_csv(free_path)
    df_fixed = pd.read_csv(fixed_path)

    plt.figure(figsize=(9, 6))

    # Fixed params for each curve (everything except the varying N)
    free_params = params_text(df_free, exclude=("N",))
    fixed_params = params_text(df_fixed, exclude=("N", "L", "M"))

    # Plot Free Density (Section 4.1)
    # Changed label to be short so it doesn't clutter the legend
    plt.errorbar(
        df_free["N"],
        df_free["mean_time_ms"],
        yerr=df_free["std_dev_ms"],
        fmt='-o',
        label="Free Density",
        capsize=5,
        capthick=1.5,
        color=sns.color_palette()[0]
    )

    # Plot Fixed Density (Section 4.2)
    # Changed label to be short so it doesn't clutter the legend
    plt.errorbar(
        df_fixed["N"],
        df_fixed["mean_time_ms"],
        yerr=df_fixed["std_dev_ms"],
        fmt='-s',
        label="Fixed Density",
        capsize=5,
        capthick=1.5,
        color=sns.color_palette()[1]
    )

    plt.title("Execution Time vs N (Free vs Fixed Density)", fontsize=14, fontweight="bold")
    plt.xlabel("N (Number of Particles)")
    plt.ylabel("Mean Execution Time (ms)")
    plt.yscale("log")

    # Add legend to distinguish the overlapping curves
    plt.legend(title="Density Configuration", loc="upper left", fontsize=10, title_fontsize=11)

    # Combine both parameter strings into a single text box with a newline (\n)
    # Placed at the bottom right (x=0.97, ha="right") to avoid the data lines
    ax = plt.gca()
    combined_params_text = f"Free Density Params: {free_params}\nFixed Density Params: {fixed_params}"
    add_params_box(ax, combined_params_text, x=0.97, y=0.04, ha="right")

    print(f"  Free Density params:  {free_params}")
    print(f"  Fixed Density params: {fixed_params}")

    plt.tight_layout()
    combined_save_path = os.path.join(output_dir, "variation_n_combined.png")
    plt.savefig(combined_save_path, dpi=300)
    plt.close()

    print(f"Saved combined figure: {combined_save_path}")
else:
    print(
        f"Notice: Missing one or both Variation N files in {telemetry_dir}/. Cannot plot the combined superimposed figure.")

print("All figures successfully saved in figures/")
