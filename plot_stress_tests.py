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

    m_match = re.search(r"variation_m_N(\d+)", filename, re.IGNORECASE)
    if m_match:
        n_val = m_match.group(1)
        title = f"Execution Time vs M (N={n_val})"

        plt.figure(figsize=(8, 6))

        plt.errorbar(
            df["M"],
            df["mean_time_ms"],
            yerr=df["std_dev_ms"],
            fmt='-o',
            capsize=5,
            capthick=1.5,
            color=sns.color_palette()[0]
        )

        plt.title(title, fontsize=14, fontweight="bold")
        plt.xlabel("M (Grid Divisions)")
        plt.ylabel("Mean Execution Time (ms)")

        # Double log scale (Log-Log)
        plt.xscale("log")
        plt.yscale("log")

        add_params_box(plt.gca(), params_text(df, exclude=("M",)))
        print(f"  [{filename}] Fixed params: {params_text(df, exclude=('M',))}")

        plt.tight_layout()
        save_path = os.path.join(output_dir, f"{filename}.png")
        plt.savefig(save_path, dpi=300)
        plt.close()

        print(f"Saved figure: {save_path}")

# ====================================================================
# 2. Process and Plot Variation of N (Free Combined & Fixed Combined)
# ====================================================================
n_comparisons = [
    {
        "title_mode": "Free Density",
        "std_file": "variation_n_free_density.csv",
        "bf_file": "variation_n_free_density_bf.csv",
        "output": "variation_n_free_combined.png",
        "exclude_params": ("N",)
    },
    {
        "title_mode": "Fixed Density",
        "std_file": "variation_n_fixed_density.csv",
        "bf_file": "variation_n_fixed_density_bf.csv",
        "output": "variation_n_fixed_combined.png",
        "exclude_params": ("N", "L", "M")
    }
]

for comp in n_comparisons:
    std_path = os.path.join(telemetry_dir, comp["std_file"])
    bf_path = os.path.join(telemetry_dir, comp["bf_file"])

    if os.path.exists(std_path) and os.path.exists(bf_path):
        df_std = pd.read_csv(std_path)
        df_bf = pd.read_csv(bf_path)

        plt.figure(figsize=(9, 6))

        std_params = params_text(df_std, exclude=comp["exclude_params"])
        bf_params = params_text(df_bf, exclude=comp["exclude_params"])

        # Plot Standard Algorithm
        plt.errorbar(
            df_std["N"],
            df_std["mean_time_ms"],
            yerr=df_std["std_dev_ms"],
            fmt='-o',
            label="Standard",
            capsize=5,
            capthick=1.5,
            color=sns.color_palette()[0]
        )

        # Plot Brute Force Algorithm
        plt.errorbar(
            df_bf["N"],
            df_bf["mean_time_ms"],
            yerr=df_bf["std_dev_ms"],
            fmt='-s',
            label="Brute Force",
            capsize=5,
            capthick=1.5,
            color=sns.color_palette()[3]
        )

        plt.title(f"Execution Time vs N ({comp['title_mode']}: Standard vs Brute Force)", fontsize=14, fontweight="bold")
        plt.xlabel("N (Number of Particles)")
        plt.ylabel("Mean Execution Time (ms)")

        # Double log scale (Log-Log)
        plt.xscale("log")
        plt.yscale("log")

        plt.legend(title="Algorithm Implementation", loc="upper left", fontsize=10, title_fontsize=11)

        ax = plt.gca()
        combined_params_text = f"Standard Params: {std_params}\nBrute Force Params: {bf_params}"
        add_params_box(ax, combined_params_text, x=0.97, y=0.04, ha="right")

        print(f"[{comp['title_mode']}] Standard params:    {std_params}")
        print(f"[{comp['title_mode']}] Brute Force params: {bf_params}")

        plt.tight_layout()
        combined_save_path = os.path.join(output_dir, comp["output"])
        plt.savefig(combined_save_path, dpi=300)
        plt.close()

        print(f"Saved figure: {combined_save_path}")
    else:
        print(
            f"Notice: Missing one or both files ({comp['std_file']}, {comp['bf_file']}) in {telemetry_dir}/. Skipping comparison."
        )

print("All figures successfully saved in figures/")