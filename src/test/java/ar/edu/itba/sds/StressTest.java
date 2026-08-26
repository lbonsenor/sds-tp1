package ar.edu.itba.sds;

import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.model.flocking.FlockingModel;
import ar.edu.itba.sds.model.telemetry.ExecutionTime;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.utils.CsvExporter;
import ar.edu.itba.sds.utils.RandomParticleGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class StressTest {
    private static final DateTimeFormatter RUN_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    private static final float L = 20f;
    private static final float RI_MIN = 0.23f;
    private static final float RI_MAX = 0.26f;
    private static final float RC = 1.0f;
    private static final boolean CONTOUR = false;

    private static final int GLOBAL_WARMUP_RUNS = 2000;
    private static final int BENCHMARK_ITERATIONS = 1500;

    private static final int SEED = 47;

    // Intermediate N used in Section 3, also defines the intermediate density of Section 4.1
    private static final int INTERMEDIATE_N = 400;

    // Section 4.2 keeps density constant at the value of N=400 with L=20 (rho = 400 / 20^2 = 1.0)
    private static final float TARGET_DENSITY = (float) INTERMEDIATE_N / (L * L);

    // Cached across test methods (JUnit creates a new instance per method)
    private static Integer maxFeasibleNCache;

    /**
     * Data holder for mean and standard deviation telemetry.
     */
    public static class BenchmarkResult {
        public final double mean;
        public final double stdDev;

        public BenchmarkResult(double mean, double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }
    }

    /**
     * Section 3: Variation of M
     */
    @Test
    @DisplayName("Stress Test: Variation of M (Section 3)")
    void testVariationOfM() {
        final String runId = RUN_ID_FORMATTER.format(Instant.now());

        Random random = new Random(SEED);

        int maxN = findMaxFeasibleN(L);
        int[] nValues = {INTERMEDIATE_N, maxN};

        double maxCellSizeLimit = RC + 2 * RI_MAX;
        int maxM = (int) Math.floor(L / maxCellSizeLimit);

        List<ExecutionTime> telemetryList = new ArrayList<>();

        for (int n : nValues) {
            Set<SizedParticle> particles = generateParticles(n, L, random.nextInt());

            globalJitWarmup(L, particles);

            for (int m = 1; m <= maxM; m++) {
                CellIndexService<SizedParticle> service = new CellIndexService<>(m, L, RC, particles);

                BenchmarkResult result = runBenchmark(service, particles);

                double density = n / (double) (L * L);
                double meanSec = result.mean / 1000.0;

                telemetryList.add(new ExecutionTime(runId,
                        FlockingModel.STANDARD, density, n, "CIM_M" + m,
                        meanSec, BENCHMARK_ITERATIONS, L, RC
                ));

                System.out.printf("[Variation M] N: %d | M: %2d/%d | Mean: %.4f ms | StdDev: %.4f ms%n",
                        n, m, maxM, result.mean, result.stdDev);
            }
        }

        CsvExporter.exportTelemetry(telemetryList, "variation_m_execution_times.csv");
    }

    /**
     * Section 4.1: Variation of N with Free Density
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Free Density (Section 4.1)")
    void testVariationOfNFreeDensity() {
        final String runId = RUN_ID_FORMATTER.format(Instant.now());

        int optimalM = (int) Math.floor(L / (RC + 2 * RI_MAX));
        int maxN = findMaxFeasibleN(L);

        int[] nValues = {10, 50, 100, 200, 300, INTERMEDIATE_N, 500, 600, 700, 800, 900, 1000, maxN};

        Random random = new Random(SEED);
        List<ExecutionTime> telemetryList = new ArrayList<>();

        for (int n : nValues) {
            Set<SizedParticle> particles = generateParticles(n, L, random.nextInt());

            globalJitWarmup(L, particles);

            CellIndexService<SizedParticle> service = new CellIndexService<>(optimalM, L, RC, particles);

            BenchmarkResult result = runBenchmark(service, particles);

            double density = n / (double) (L * L);
            double meanSec = result.mean / 1000.0;

            telemetryList.add(new ExecutionTime(runId,
                    FlockingModel.STANDARD, density, n, "CIM",
                    meanSec, BENCHMARK_ITERATIONS, L, RC
            ));

            System.out.printf("[Variation N - Free Density] N: %d | L: %.0f | M: %d | Mean: %.4f ms | StdDev: %.4f ms%n",
                    n, L, optimalM, result.mean, result.stdDev);
        }

        CsvExporter.exportTelemetry(telemetryList, "variation_n_free_density.csv");
    }

    /**
     * Section 4.2: Variation of N with Fixed Density
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Fixed Density (Section 4.2)")
    void testVariationOfNFixedDensity() {
        final String runId = RUN_ID_FORMATTER.format(Instant.now());

        int[] nValues = {50, 100, 200, INTERMEDIATE_N, 600, 800, 1000, 1200, 1600};

        Random random = new Random(SEED);
        List<ExecutionTime> telemetryList = new ArrayList<>();

        for (int n : nValues) {
            int l = (int) Math.round(Math.sqrt(n / TARGET_DENSITY));
            float actualDensity = (float) n / (l * l);
            int m = (int) Math.floor(l / (RC + 2 * RI_MAX));

            Set<SizedParticle> particles = generateParticles(n, l, random.nextInt());

            globalJitWarmup(l, particles);

            CellIndexService<SizedParticle> service = new CellIndexService<>(m, l, RC, particles);

            BenchmarkResult result = runBenchmark(service, particles);

            double meanSec = result.mean / 1000.0;

            telemetryList.add(new ExecutionTime(runId,
                    FlockingModel.STANDARD, actualDensity, n, "CIM",
                    meanSec, BENCHMARK_ITERATIONS, l, RC
            ));

            System.out.printf("[Variation N - Fixed Density] N: %d | L: %d | M: %d | Density: %.4f | Mean: %.4f ms | StdDev: %.4f ms%n",
                    n, l, m, actualDensity, result.mean, result.stdDev);
        }

        CsvExporter.exportTelemetry(telemetryList, "variation_n_fixed_density.csv");
    }

    /**
     * Section 4.1: Variation of N with Free Density (Brute Force)
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Free Density Brute Force (Section 4.1)")
    void testVariationOfNFreeDensityBruteForce() {
        final String runId = RUN_ID_FORMATTER.format(Instant.now());

        int optimalM = 1;
        int maxN = findMaxFeasibleN(L);

        int[] nValues = {10, 50, 100, 200, 300, INTERMEDIATE_N, 500, 600, 700, 800, 900, maxN};

        Random random = new Random(SEED);
        List<ExecutionTime> telemetryList = new ArrayList<>();

        for (int n : nValues) {
            Set<SizedParticle> particles = generateParticles(n, L, random.nextInt());

            globalJitWarmup(L, particles);

            CellIndexService<SizedParticle> service = new CellIndexService<>(optimalM, L, RC, particles);

            BenchmarkResult result = runBenchmark(service, particles);

            double density = n / (double) (L * L);
            double meanSec = result.mean / 1000.0;

            telemetryList.add(new ExecutionTime(runId,
                    FlockingModel.STANDARD, density, n, "brute_force",
                    meanSec, BENCHMARK_ITERATIONS, L, RC
            ));

            System.out.printf("[Variation N - Free Density BF] N: %d | L: %.0f | M: %d | Mean: %.4f ms | StdDev: %.4f ms%n",
                    n, L, optimalM, result.mean, result.stdDev);
        }

        CsvExporter.exportTelemetry(telemetryList, "variation_n_free_density_bf.csv");
    }

    /**
     * Section 4.2: Variation of N with Fixed Density (Brute Force)
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Fixed Density Brute Force (Section 4.2)")
    void testVariationOfNFixedDensityBruteForce() {
        final String runId = RUN_ID_FORMATTER.format(Instant.now());
        int[] nValues = {50, 100, 200, INTERMEDIATE_N, 600, 800, 1000, 1200, 1600};

        Random random = new Random(SEED);
        List<ExecutionTime> telemetryList = new ArrayList<>();

        for (int n : nValues) {
            int l = (int) Math.round(Math.sqrt(n / TARGET_DENSITY));
            float actualDensity = (float) n / (l * l);
            int m = 1;

            Set<SizedParticle> particles = generateParticles(n, l, random.nextInt());

            globalJitWarmup(l, particles);

            CellIndexService<SizedParticle> service = new CellIndexService<>(m, l, RC, particles);

            BenchmarkResult result = runBenchmark(service, particles);

            double meanSec = result.mean / 1000.0;

            telemetryList.add(new ExecutionTime(runId,
                    FlockingModel.STANDARD, actualDensity, n, "brute_force",
                    meanSec, BENCHMARK_ITERATIONS, l, RC
            ));

            System.out.printf("[Variation N - Fixed Density BF] N: %d | L: %d | M: %d | Density: %.4f | Mean: %.4f ms | StdDev: %.4f ms%n",
                    n, l, m, actualDensity, result.mean, result.stdDev);
        }

        CsvExporter.exportTelemetry(telemetryList, "variation_n_fixed_density_bf.csv");
    }

    /**
     * Determines the highest N that can be generated in a box of side {@code l} without overlapping.
     */
    private int findMaxFeasibleN(float l) {
        if (maxFeasibleNCache != null) {
            return maxFeasibleNCache;
        }

        int start = 1000;
        int step = 50;
        int cap = 2000;
        int best = start;
        Random random = new Random(SEED);

        for (int candidate = start; candidate <= cap; candidate += step) {
            if (canGenerateReliably(candidate, l, random.nextInt())) {
                best = candidate;
            } else {
                break;
            }
        }

        if (best > start) {
            best -= step;
        }

        maxFeasibleNCache = best;
        System.out.println("[StressTest] Max feasible N for L=" + l + ": " + best);
        return best;
    }

    private boolean canGenerateReliably(int n, float l, int seed) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX, seed);
            } catch (IllegalStateException e) {
                return false;
            }
        }
        return true;
    }

    private Set<SizedParticle> generateParticles(int n, float l, int seed) {
        IllegalStateException lastFailure = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX, seed);
            } catch (IllegalStateException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }

    private void globalJitWarmup(float l, Set<SizedParticle> particles) {
        CellIndexService<SizedParticle> warmupService = new CellIndexService<>(3, l, StressTest.RC, particles);
        for (int i = 0; i < GLOBAL_WARMUP_RUNS; i++) {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }
            warmupService.calculateNeighbors(CONTOUR, particles);
        }
    }

    private BenchmarkResult runBenchmark(CellIndexService<SizedParticle> service,
                                         Set<SizedParticle> particles) {

        double[] runTimesMs = new double[BENCHMARK_ITERATIONS];

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }

            long startNano = System.nanoTime();
            service.calculateNeighbors(CONTOUR, particles);
            long elapsedNano = System.nanoTime() - startNano;

            runTimesMs[i] = elapsedNano / 1_000_000.0;
        }

        double sum = 0;
        for (double time : runTimesMs) {
            sum += time;
        }
        double mean = sum / BENCHMARK_ITERATIONS;

        double varianceSum = 0;
        for (double time : runTimesMs) {
            varianceSum += Math.pow(time - mean, 2);
        }
        double stdDev = Math.sqrt(varianceSum / BENCHMARK_ITERATIONS);

        return new BenchmarkResult(mean, stdDev);
    }
}