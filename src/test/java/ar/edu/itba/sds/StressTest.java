package ar.edu.itba.sds;

import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.utils.CsvExporter;
import ar.edu.itba.sds.utils.RandomParticleGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

public class StressTest {

    private static final float RI_MIN = 0.23f;
    private static final float RI_MAX = 0.26f;
    private static final float RC = 1.0f;

    private static final int GLOBAL_WARMUP_RUNS = 1000;
    private static final int BENCHMARK_ITERATIONS = 200;

    /**
     * Section 3: Variation of M
     */
    @Test
    @DisplayName("Stress Test: Variation of M (Section 3)")
    void testVariationOfM() {
        int l = 20;
        int[] nValues = {1000, 1100};
        boolean contour = false;

        double maxCellSizeLimit = RC + 2 * RI_MAX;
        int maxM = (int) Math.floor(l / maxCellSizeLimit);

        for (int n : nValues) {
            Set<SizedParticle> particles = RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX);

            // Force JIT compilation before benchmarking this particle set
            globalJitWarmup(l, particles, contour);

            for (int m = 1; m <= maxM; m++) {
                CellIndexService<SizedParticle> service = new CellIndexService<>(m, l, RC, particles);

                double medianTimeMs = runBenchmark(service, particles, contour);

                CsvExporter.exportVariationMTelemetry(n, m, medianTimeMs);

                System.out.printf("[Variation M] N: %d | M: %2d/%d | Median Time: %.4f ms%n",
                        n, m, maxM, medianTimeMs);
            }
        }
    }

    /**
     * Section 4.1: Variation of N with Free Density
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Free Density (Section 4.1)")
    void testVariationOfNFreeDensity() {
        int l = 20;
        int optimalM = (int) Math.floor(l / (RC + 2 * RI_MAX));
        boolean contour = false;

        int[] nValues = {10, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500};

        for (int n : nValues) {
            Set<SizedParticle> particles = RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX);

            globalJitWarmup(l, particles, contour);

            CellIndexService<SizedParticle> service = new CellIndexService<>(optimalM, l, RC, particles);

            double medianTimeMs = runBenchmark(service, particles, contour);

            CsvExporter.exportVariationNFreeDensityTelemetry(n, medianTimeMs);

            System.out.printf("[Variation N - Free Density] N: %d | M: %d | Median Time: %.4f ms%n",
                    n, optimalM, medianTimeMs);
        }
    }

    /**
     * Section 4.2: Variation of N with Fixed Density
     */
    @Test
    @DisplayName("Stress Test: Variation of N - Fixed Density (Section 4.2)")
    void testVariationOfNFixedDensity() {
        float targetDensity = 0.5f;
        int[] nValues = {50, 100, 200, 400, 600, 800};
        boolean contour = false;

        for (int n : nValues) {
            int l = (int) Math.round(Math.sqrt(n / targetDensity));
            float actualDensity = (float) n / (l * l);
            int m = (int) Math.floor(l / (RC + 2 * RI_MAX));

            Set<SizedParticle> particles = RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX);

            globalJitWarmup(l, particles, contour);

            CellIndexService<SizedParticle> service = new CellIndexService<>(m, l, RC, particles);

            double medianTimeMs = runBenchmark(service, particles, contour);

            CsvExporter.exportVariationNFixedDensityTelemetry(n, l, m, actualDensity, medianTimeMs);

            System.out.printf("[Variation N - Fixed Density] N: %d | L: %d | M: %d | Density: %.4f | Median Time: %.4f ms%n",
                    n, l, m, actualDensity, medianTimeMs);
        }
    }

    /**
     * Executes heavy dummy iterations before recording measurements to ensure
     * the JVM JIT compiler fully optimizes and compiles bytecode to machine code.
     */
    private void globalJitWarmup(float l, Set<SizedParticle> particles, boolean contour) {
        CellIndexService<SizedParticle> warmupService = new CellIndexService<>(3, l, StressTest.RC, particles);
        for (int i = 0; i < GLOBAL_WARMUP_RUNS; i++) {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }
            warmupService.calculateNeighbors(contour);
        }
    }

    /**
     * Measures precise execution times and returns the MEDIAN runtime
     * to eliminate garbage collection and OS context-switch noise.
     */
    private double runBenchmark(CellIndexService<SizedParticle> service,
                                Set<SizedParticle> particles,
                                boolean contour) {

        double[] runTimesMs = new double[BENCHMARK_ITERATIONS];

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }

            long startNano = System.nanoTime();
            service.calculateNeighbors(contour);
            long elapsedNano = System.nanoTime() - startNano;

            runTimesMs[i] = elapsedNano / 1_000_000.0;
        }

        Arrays.sort(runTimesMs);
        return runTimesMs[BENCHMARK_ITERATIONS / 2];
    }
}