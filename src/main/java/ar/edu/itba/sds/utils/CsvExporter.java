package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CsvExporter {

    private static final String OUT_DIR = "out";
    private static final String TELEMETRY_DIR = "telemetry";

    public static void exportParticleData(Collection<SizedParticle> particles, int timestep) {
        List<SizedParticle> particleList = new ArrayList<>(particles);
        ensureDirectoryExists(OUT_DIR);

        exportStaticData(particleList);
        exportDynamicData(particleList, timestep);
    }

    private static void exportStaticData(List<SizedParticle> particleList) {
        File staticFile = new File(OUT_DIR + "/static_data.csv");
        try (FileWriter writer = new FileWriter(staticFile)) {
            writer.write("ID,radius\n");
            for (int i = 0; i < particleList.size(); i++) {
                SizedParticle p = particleList.get(i);
                float r = (p.getMaxX() - p.getMinX()) / 2.0f;
                writer.write(String.format(Locale.US, "%d,%.4f\n", i, r));
            }
        } catch (IOException e) {
            System.err.println("Error writing static CSV: " + e.getMessage());
        }
    }

    private static void exportDynamicData(List<SizedParticle> particleList, int timestep) {
        File dynamicFile = new File(OUT_DIR + "/dynamic_data.csv");
        boolean appendMode = timestep > 0;

        try (FileWriter writer = new FileWriter(dynamicFile, appendMode)) {
            if (timestep == 0) {
                writer.write("t,ID,Xpos,Ypos,neighbours\n");
            }

            for (int i = 0; i < particleList.size(); i++) {
                SizedParticle p = particleList.get(i);
                float x = (p.getMaxX() + p.getMinX()) / 2.0f;
                float y = (p.getMaxY() + p.getMinY()) / 2.0f;

                String neighborsStr = p.getNeighbors().stream()
                        .map(neighbor -> String.valueOf(particleList.indexOf(neighbor)))
                        .collect(Collectors.joining(" "));

                if (neighborsStr.isEmpty()) {
                    neighborsStr = "nadie";
                } else {
                    neighborsStr = "[" + neighborsStr + "]";
                }

                writer.write(String.format(Locale.US, "%d,%d,%.4f,%.4f,%s\n", timestep, i, x, y, neighborsStr));
            }
        } catch (IOException e) {
            System.err.println("Error writing dynamic CSV: " + e.getMessage());
        }
    }

    /**
     * Standard execution telemetry writer for single/manual runs.
     */
    public static void exportExecutionTelemetry(int n, int l, int m, float rc,
                                                float riMin, float riMax, long executionTimeMs) {
        ensureDirectoryExists(TELEMETRY_DIR);
        File executionFile = new File(TELEMETRY_DIR + "/execution_data.csv");
        boolean fileExists = executionFile.exists();

        try (FileWriter writer = new FileWriter(executionFile, true)) {
            if (!fileExists) {
                writer.write("N,L,M,Rc,riMin,riMax,density,execution_time_ms\n");
            }

            float density = (float) n / (l * l);
            writer.write(String.format(Locale.US, "%d,%d,%d,%.2f,%.2f,%.2f,%.4f,%d\n",
                    n, l, m, rc, riMin, riMax, density, executionTimeMs));

        } catch (IOException e) {
            System.err.println("Error writing execution CSV: " + e.getMessage());
        }
    }

    public static void exportVariationMTelemetry(int n, int m, double executionTimeMs) {
        ensureDirectoryExists(TELEMETRY_DIR);
        File file = new File(TELEMETRY_DIR + "/variation_m_N" + n + ".csv");
        boolean fileExists = file.exists();

        try (FileWriter writer = new FileWriter(file, true)) {
            if (!fileExists) {
                writer.write("M,execution_time_ms\n");
            }
            writer.write(String.format(Locale.US, "%d,%.4f\n", m, executionTimeMs));
        } catch (IOException e) {
            System.err.println("Error writing variation M CSV: " + e.getMessage());
        }
    }

    public static void exportVariationNFreeDensityTelemetry(int n, double executionTimeMs) {
        ensureDirectoryExists(TELEMETRY_DIR);
        File file = new File(TELEMETRY_DIR + "/variation_n_free_density.csv");
        boolean fileExists = file.exists();

        try (FileWriter writer = new FileWriter(file, true)) {
            if (!fileExists) {
                writer.write("N,execution_time_ms\n");
            }
            writer.write(String.format(Locale.US, "%d,%.4f\n", n, executionTimeMs));
        } catch (IOException e) {
            System.err.println("Error writing variation N free density CSV: " + e.getMessage());
        }
    }

    public static void exportVariationNFixedDensityTelemetry(int n, int l, int m, float density, double executionTimeMs) {
        ensureDirectoryExists(TELEMETRY_DIR);
        File file = new File(TELEMETRY_DIR + "/variation_n_fixed_density.csv");
        boolean fileExists = file.exists();

        try (FileWriter writer = new FileWriter(file, true)) {
            if (!fileExists) {
                writer.write("N,L,M,density,execution_time_ms\n");
            }
            writer.write(String.format(Locale.US, "%d,%d,%d,%.4f,%.4f\n", n, l, m, density, executionTimeMs));
        } catch (IOException e) {
            System.err.println("Error writing variation N fixed density CSV: " + e.getMessage());
        }
    }

    private static void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }
}