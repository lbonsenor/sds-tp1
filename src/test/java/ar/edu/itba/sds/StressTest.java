package ar.edu.itba.sds;

import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.utils.CsvExporter;
import ar.edu.itba.sds.utils.RandomParticleGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class StressTest {

    private static final float L = 20f;
    private static final float RI_MIN = 0.23f;
    private static final float RI_MAX = 0.26f;
    private static final float RC = 1.0f;
    private static final boolean CONTOUR = false;

    private static final int GLOBAL_WARMUP_RUNS = 1000;
    private static final int BENCHMARK_ITERATIONS = 200;

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
        int maxN = findMaxFeasibleN(L);
        int[] nValues = {INTERMEDIATE_N, maxN};

        double maxCellSizeLimit = RC + 2 * RI_MAX;
        int maxM = (int) Math.floor(L / maxCellSizeLimit);

        for (int n : nValues) {
            Set<SizedParticle> particles = generateParticles(n, L);

            globalJitWarmup(L, particles);

            for (int m = 1; m <= maxM; m++) {
                CellIndexService<SizedParticle> service = new CellIndexService<>(m, L, RC, particles);

                BenchmarkResult result = runBenchmark(service, particles);

                CsvExporter.exportVariationMTelemetry(n, L, RC, RI_MIN, RI_MAX, CONTOUR,
                        GLOBAL_WARMUP_RUNS, BENCHMARK_ITERATIONS, m, result.mean, result.stdDev);

                System.out.printf("[Variation M] N: %d | M: %2d/%d | Mean: %.4f ms | StdDev: %.4f ms%n",
                        n, m, maxM, result.mean, result.stdDev);
            }
        }
    }

    /**
     * Section 4.1: Variation of N with Free Density
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Free Density (Section 4.1)")
    void testVariationOfNFreeDensity() {
        int optimalM = (int) Math.floor(L / (RC + 2 * RI_MAX));
        int maxN = findMaxFeasibleN(L);

        int[] nValues = {10, 50, 100, 200, 300, INTERMEDIATE_N, 500, 600, 700, 800, 900, 1000, maxN};




        for (int n : nValues) {
            Set<SizedParticle> particles = generateParticles(n, L);

            globalJitWarmup(L, particles);

            CellIndexService<SizedParticle> service = new CellIndexService<>(optimalM, L, RC, particles);

            BenchmarkResult result = runBenchmark(service, particles);

            CsvExporter.exportVariationNFreeDensityTelemetry(n, L, optimalM, RC, RI_MIN, RI_MAX, CONTOUR,
                    GLOBAL_WARMUP_RUNS, BENCHMARK_ITERATIONS, result.mean, result.stdDev);

            System.out.printf("[Variation N - Free Density] N: %d | L: %.0f | M: %d | Mean: %.4f ms | StdDev: %.4f ms%n",
                    n, L, optimalM, result.mean, result.stdDev);
        }
    }

    /**
     * Section 4.2: Variation of N with Fixed Density
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Fixed Density (Section 4.2)")
    void testVariationOfNFixedDensity() {
        int[] nValues = {50, 100, 200, INTERMEDIATE_N, 600, 800, 1000, 1200, 1600};

        for (int n : nValues) {
            int l = (int) Math.round(Math.sqrt(n / TARGET_DENSITY));
            float actualDensity = (float) n / (l * l);
            int m = (int) Math.floor(l / (RC + 2 * RI_MAX));

            Set<SizedParticle> particles = generateParticles(n, l);

            globalJitWarmup(l, particles);

            CellIndexService<SizedParticle> service = new CellIndexService<>(m, l, RC, particles);

            BenchmarkResult result = runBenchmark(service, particles);

            CsvExporter.exportVariationNFixedDensityTelemetry(n, l, m, actualDensity, RC, RI_MIN, RI_MAX, CONTOUR,
                    GLOBAL_WARMUP_RUNS, BENCHMARK_ITERATIONS, result.mean, result.stdDev);

            System.out.printf("[Variation N - Fixed Density] N: %d | L: %d | M: %d | Density: %.4f | Mean: %.4f ms | StdDev: %.4f ms%n",
                    n, l, m, actualDensity, result.mean, result.stdDev);
        }
    }

    /**
     * Determines the highest N that can be generated in a box of side {@code l} without overlapping.
     * Probes increasing values of N (with a few retries to absorb sampling variance) until the
     * generator fails, then returns the last successful N.
     */
    private int findMaxFeasibleN(float l) {
        if (maxFeasibleNCache != null) {
            return maxFeasibleNCache;
        }

        int start = 1000;
        int step = 50;
        int cap = 2000;
        int best = start;

        for (int candidate = start; candidate <= cap; candidate += step) {
            if (canGenerateReliably(candidate, l)) {
                best = candidate;
            } else {
                break;
            }
        }

        // Step back one notch to stay clear of the probabilistic packing boundary.
        if (best > start) {
            best -= step;
        }

        maxFeasibleNCache = best;
        System.out.println("[StressTest] Max feasible N for L=" + l + ": " + best);
        return best;
    }

    /**
     * True only if every attempt generated all {@code n} particles, filtering out sampling variance
     * near the maximum packing density.
     */
    private boolean canGenerateReliably(int n, float l) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX);
            } catch (IllegalStateException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generates {@code n} particles, retrying with fresh random seeds to absorb sampling variance.
     */
    private Set<SizedParticle> generateParticles(int n, float l) {
        IllegalStateException lastFailure = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX);
            } catch (IllegalStateException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }

    /**
     * Executes heavy dummy iterations before recording measurements to ensure
     * the JVM JIT compiler fully optimizes and compiles bytecode to machine code.
     */
    private void globalJitWarmup(float l, Set<SizedParticle> particles) {
        CellIndexService<SizedParticle> warmupService = new CellIndexService<>(3, l, StressTest.RC, particles);
        for (int i = 0; i < GLOBAL_WARMUP_RUNS; i++) {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }
            warmupService.calculateNeighbors(CONTOUR);
        }
    }

    /**
     * Measures precise execution times and returns the Mean and Standard Deviation.
     */
    private BenchmarkResult runBenchmark(CellIndexService<SizedParticle> service,
                                         Set<SizedParticle> particles) {

        double[] runTimesMs = new double[BENCHMARK_ITERATIONS];

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }

            long startNano = System.nanoTime();
            service.calculateNeighbors(CONTOUR);
            long elapsedNano = System.nanoTime() - startNano;

            runTimesMs[i] = elapsedNano / 1_000_000.0;
        }

        // 1. Calculate Mean
        double sum = 0;
        for (double time : runTimesMs) {
            sum += time;
        }
        double mean = sum / BENCHMARK_ITERATIONS;

        // 2. Calculate Standard Deviation
        double varianceSum = 0;
        for (double time : runTimesMs) {
            varianceSum += Math.pow(time - mean, 2);
        }
        double stdDev = Math.sqrt(varianceSum / BENCHMARK_ITERATIONS);

        return new BenchmarkResult(mean, stdDev);
    }
}
