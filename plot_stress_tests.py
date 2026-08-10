import os
import glob
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Set overall seaborn aesthetic style
sns.set_theme(style="whitegrid", palette="deep")

# Ensure output directory exists
output_dir = "figures"
os.makedirs(output_dir, exist_ok=True)

# Define file paths
data_files = {
    "m_N100": "telemetry/variation_m_N100.csv",
    "m_N300": "telemetry/variation_m_N300.csv",
    "n_free": "telemetry/variation_n_free_density.csv",
    "n_fixed": "telemetry/variation_n_fixed_density.csv",
}

# Load datasets into a dictionary
dfs = {}
for key, path in data_files.items():
    if os.path.exists(path):
        dfs[key] = pd.read_csv(path)
    else:
        print(f"Warning: File {path} not found.")

# 1. Individual Plot: Variation M (N=100)
if "m_N100" in dfs:
    plt.figure(figsize=(7, 5))
    sns.lineplot(data=dfs["m_N100"], x="M", y="execution_time_ms", marker="o", color="blue", errorbar=None)
    plt.title("Execution Time vs M (N=100)", fontsize=12, fontweight="bold")
    plt.xlabel("M")
    plt.ylabel("Execution Time (ms)")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "variation_m_N100.png"), dpi=300)
    plt.close()

# 2. Individual Plot: Variation M (N=300)
if "m_N300" in dfs:
    plt.figure(figsize=(7, 5))
    sns.lineplot(data=dfs["m_N300"], x="M", y="execution_time_ms", marker="o", color="orange", errorbar=None)
    plt.title("Execution Time vs M (N=300)", fontsize=12, fontweight="bold")
    plt.xlabel("M")
    plt.ylabel("Execution Time (ms)")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "variation_m_N300.png"), dpi=300)
    plt.close()

# 3. Individual Plot: Variation N (Free Density)
if "n_free" in dfs:
    plt.figure(figsize=(7, 5))
    sns.lineplot(data=dfs["n_free"], x="N", y="execution_time_ms", marker="o", color="green", errorbar=None)
    plt.title("Execution Time vs N (Free Density)", fontsize=12, fontweight="bold")
    plt.xlabel("N")
    plt.ylabel("Execution Time (ms)")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "variation_n_free_density.png"), dpi=300)
    plt.close()

# 4. Individual Plot: Variation N (Fixed Density)
if "n_fixed" in dfs:
    plt.figure(figsize=(7, 5))
    sns.lineplot(data=dfs["n_fixed"], x="N", y="execution_time_ms", marker="o", color="purple", errorbar=None)
    plt.title("Execution Time vs N (Fixed Density)", fontsize=12, fontweight="bold")
    plt.xlabel("N")
    plt.ylabel("Execution Time (ms)")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "variation_n_fixed_density.png"), dpi=300)
    plt.close()

print("All figures successfully saved in figures/")