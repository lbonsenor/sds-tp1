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
# 2. Process and Plot Variation of N Comparisons
# ====================================================================
n_comparisons = [
    # 1. Standard Algorithm: Free vs Fixed Density
    {
        "title": "Execution Time vs N (Standard: Free vs Fixed Density)",
        "legend_title": "Density Configuration",
        "file1": "variation_n_free_density.csv",
        "label1": "Free Density",
        "fmt1": "-o",
        "exclude1": ("N",),
        "file2": "variation_n_fixed_density.csv",
        "label2": "Fixed Density",
        "fmt2": "-s",
        "exclude2": ("N", "L", "M"),
        "color2_idx": 1,
        "output": "variation_n_combined.png"
    },
    # 2. Brute Force Algorithm: Free vs Fixed Density
    {
        "title": "Execution Time vs N (Brute Force: Free vs Fixed Density)",
        "legend_title": "Density Configuration",
        "file1": "variation_n_free_density_bf.csv",
        "label1": "Free Density",
        "fmt1": "-o",
        "exclude1": ("N",),
        "file2": "variation_n_fixed_density_bf.csv",
        "label2": "Fixed Density",
        "fmt2": "-s",
        "exclude2": ("N", "L", "M"),
        "color2_idx": 1,
        "output": "variation_n_combined_bf.png"
    },
    # 3. Free Density: Standard vs Brute Force
    {
        "title": "Execution Time vs N (Free Density: Standard vs Brute Force)",
        "legend_title": "Algorithm",
        "file1": "variation_n_free_density.csv",
        "label1": "Standard",
        "fmt1": "-o",
        "exclude1": ("N",),
        "file2": "variation_n_free_density_bf.csv",
        "label2": "Brute Force",
        "fmt2": "-s",
        "exclude2": ("N",),
        "color2_idx": 3,
        "output": "variation_n_free_combined.png"
    },
    # 4. Fixed Density: Standard vs Brute Force
    {
        "title": "Execution Time vs N (Fixed Density: Standard vs Brute Force)",
        "legend_title": "Algorithm",
        "file1": "variation_n_fixed_density.csv",
        "label1": "Standard",
        "fmt1": "-o",
        "exclude1": ("N", "L", "M"),
        "file2": "variation_n_fixed_density_bf.csv",
        "label2": "Brute Force",
        "fmt2": "-s",
        "exclude2": ("N", "L", "M"),
        "color2_idx": 3,
        "output": "variation_n_fixed_combined.png"
    }
]

for comp in n_comparisons:
    path1 = os.path.join(telemetry_dir, comp["file1"])
    path2 = os.path.join(telemetry_dir, comp["file2"])

    if os.path.exists(path1) and os.path.exists(path2):
        df1 = pd.read_csv(path1)
        df2 = pd.read_csv(path2)

        plt.figure(figsize=(9, 6))

        p1_text = params_text(df1, exclude=comp["exclude1"])
        p2_text = params_text(df2, exclude=comp["exclude2"])

        # Curve 1
        plt.errorbar(
            df1["N"],
            df1["mean_time_ms"],
            yerr=df1["std_dev_ms"],
            fmt=comp["fmt1"],
            label=comp["label1"],
            capsize=5,
            capthick=1.5,
            color=sns.color_palette()[0]
        )

        # Curve 2
        plt.errorbar(
            df2["N"],
            df2["mean_time_ms"],
            yerr=df2["std_dev_ms"],
            fmt=comp["fmt2"],
            label=comp["label2"],
            capsize=5,
            capthick=1.5,
            color=sns.color_palette()[comp["color2_idx"]]
        )

        plt.title(comp["title"], fontsize=14, fontweight="bold")
        plt.xlabel("N (Number of Particles)")
        plt.ylabel("Mean Execution Time (ms)")

        # Double log scale (Log-Log)
        plt.xscale("log")
        plt.yscale("log")

        plt.legend(title=comp["legend_title"], loc="upper left", fontsize=10, title_fontsize=11)

        ax = plt.gca()
        combined_params_text = f"{comp['label1']} Params: {p1_text}\n{comp['label2']} Params: {p2_text}"
        add_params_box(ax, combined_params_text, x=0.97, y=0.04, ha="right")

        print(f"[{comp['title']}] {comp['label1']}: {p1_text}")
        print(f"[{comp['title']}] {comp['label2']}: {p2_text}")

        plt.tight_layout()
        save_path = os.path.join(output_dir, comp["output"])
        plt.savefig(save_path, dpi=300)
        plt.close()

        print(f"Saved figure: {save_path}")
    else:
        print(
            f"Notice: Missing one or both files ({comp['file1']}, {comp['file2']}) in {telemetry_dir}/. Skipping plot."
        )

print("All figures successfully saved in figures/")