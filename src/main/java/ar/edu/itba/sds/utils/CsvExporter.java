package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class CsvExporter {

    private static final String OUT_DIR = "out";
    private static final String TELEMETRY_DIR = "telemetry";
    private static final String COMMON_FIXED_COLUMNS = "rc,riMin,riMax,contour,warmup_runs,benchmark_iterations";

    /**
     * Tracks telemetry files already initialized (header written) in this JVM run.
     * The first write truncates the file so repeated `mvn test` runs stay idempotent;
     * subsequent writes in the same run append rows.
     */
    private static final Set<String> INITIALIZED_TELEMETRY_FILES = new HashSet<>();

    private static FileWriter openTelemetryWriter(File file, String header) throws IOException {
        boolean firstWrite = INITIALIZED_TELEMETRY_FILES.add(file.getAbsolutePath());
        FileWriter writer = new FileWriter(file, !firstWrite);
        if (firstWrite) {
            writer.write(header + "\n");
        }
        return writer;
    }

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

        } catch (IOException e) {System.err.println("Error writing execution CSV: " + e.getMessage());
        }
    }

    public static void exportVariationMTelemetry(int n, float l, float rc, float riMin, float riMax,
                                                 boolean contour, int warmupRuns, int benchmarkIterations,
                                                 int m, double meanTimeMs, double stdDevMs) {
        ensureDirectoryExists(TELEMETRY_DIR);
        File file = new File(TELEMETRY_DIR + "/variation_m_N" + n + ".csv");

        try (FileWriter writer = openTelemetryWriter(file,
                "N,L,M," + COMMON_FIXED_COLUMNS + ",mean_time_ms,std_dev_ms")) {
            writer.write(String.format(Locale.US, "%d,%.0f,%d,%.2f,%.2f,%.2f,%d,%d,%d,%.4f,%.4f\n",
                    n, l, m, rc, riMin, riMax, contour ? 1 : 0, warmupRuns, benchmarkIterations, meanTimeMs, stdDevMs));
        } catch (IOException e) {
            System.err.println("Error writing variation M CSV: " + e.getMessage());
        }
    }

    public static void exportVariationNFreeDensityTelemetry(int n, float l, int m, float rc, float riMin, float riMax,
                                                            boolean contour, int warmupRuns, int benchmarkIterations,
                                                            double meanTimeMs, double stdDevMs) {
        ensureDirectoryExists(TELEMETRY_DIR);
        File file = new File(TELEMETRY_DIR + "/variation_n_free_density.csv");

        try (FileWriter writer = openTelemetryWriter(file,
                "N,L,M," + COMMON_FIXED_COLUMNS + ",mean_time_ms,std_dev_ms")) {
            writer.write(String.format(Locale.US, "%d,%.0f,%d,%.2f,%.2f,%.2f,%d,%d,%d,%.4f,%.4f\n",
                    n, l, m, rc, riMin, riMax, contour ? 1 : 0, warmupRuns, benchmarkIterations, meanTimeMs, stdDevMs));
        } catch (IOException e) {
            System.err.println("Error writing variation N free density CSV: " + e.getMessage());
        }
    }

    public static void exportVariationNFixedDensityTelemetry(int n, int l, int m, float density, float rc,
                                                             float riMin, float riMax, boolean contour,
                                                             int warmupRuns, int benchmarkIterations,
                                                             double meanTimeMs, double stdDevMs) {
        ensureDirectoryExists(TELEMETRY_DIR);
        File file = new File(TELEMETRY_DIR + "/variation_n_fixed_density.csv");

        try (FileWriter writer = openTelemetryWriter(file,
                "N,L,M,density," + COMMON_FIXED_COLUMNS + ",mean_time_ms,std_dev_ms")) {
            writer.write(String.format(Locale.US, "%d,%d,%d,%.4f,%.2f,%.2f,%.2f,%d,%d,%d,%.4f,%.4f\n",
                    n, l, m, density, rc, riMin, riMax, contour ? 1 : 0, warmupRuns, benchmarkIterations, meanTimeMs, stdDevMs));
        } catch (IOException e) {
            System.err.println("Error writing variation N fixed density CSV: " + e.getMessage());
        }
    }

    private static void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }
}