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

# Find all CSV files in the telemetry directory
csv_files = glob.glob(os.path.join(telemetry_dir, "variation_*.csv"))

if not csv_files:
    print(f"No CSV files found in {telemetry_dir}/")

for path in sorted(csv_files):
    # Extract filename without extension (e.g., 'variation_m_N10000')
    filename = os.path.splitext(os.path.basename(path))[0]
    df = pd.read_csv(path)

    # Dynamic parsing for Title and X-axis column
    m_match = re.search(r"variation_m_N(\d+)", filename, re.IGNORECASE)

    if m_match:
        # Matches files like variation_m_N100, variation_m_N10000, etc.
        n_val = m_match.group(1)
        x_col = "M"
        title = f"Execution Time vs M (N={n_val})"
    elif filename.startswith("variation_n_"):
        # Matches files like variation_n_free_density, variation_n_fixed_density, etc.
        density_label = filename.replace("variation_n_", "").replace("_", " ").title()
        x_col = "N"
        title = f"Execution Time vs N ({density_label})"
    else:
        # Fallback for unrecognized patterns
        x_col = "M" if "M" in df.columns else "N"
        title = filename.replace("_", " ").title()

    # Generate Plot
    plt.figure(figsize=(7, 5))
    sns.lineplot(
        data=df,
        x=x_col,
        y="execution_time_ms",
        marker="o",
        errorbar=None
    )
    plt.title(title, fontsize=12, fontweight="bold")
    plt.xlabel(x_col)
    plt.ylabel("Execution Time (ms)")
    plt.tight_layout()

    # Save output plot using the exact original filename base
    save_path = os.path.join(output_dir, f"{filename}.png")
    plt.savefig(save_path, dpi=300)
    plt.close()

    print(f"Saved figure: {save_path}")

print("All figures successfully saved in figures/")