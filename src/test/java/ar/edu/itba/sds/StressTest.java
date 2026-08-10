package ar.edu.itba.sds;

import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService2;
import ar.edu.itba.sds.utils.CsvExporter;
import ar.edu.itba.sds.utils.RandomParticleGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class StressTest {

    private static final float RI_MIN = 0.23f;
    private static final float RI_MAX = 0.26f;
    private static final float RC = 1.0f;

    // Increased iterations to give JIT stable measurement conditions
    private static final int WARMUP_ITERATIONS = 50;
    private static final int BENCHMARK_ITERATIONS = 100;

    /**
     * Section 3: Variation of M
     */
    @Test
    @DisplayName("Stress Test: Variation of M (Section 3)")
    void testVariationOfM() {
        int l = 20;
        int[] nValues = {100, 300}; // Intermediate and High N
        boolean contour = false;

        // Condition: L / M > rc + 2*r_max
        double maxCellSizeLimit = RC + 2 * RI_MAX;
        int maxM = (int) Math.floor(l / maxCellSizeLimit);

        for (int n : nValues) {
            Set<SizedParticle> particles = RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX);

            for (int m = 1; m <= maxM; m++) {
                CellIndexService2<SizedParticle> service = new CellIndexService2<>(m, l, RC, particles);

                // High-precision timing in fractional milliseconds (e.g. 2.43 ms)
                double avgTimeMs = runBenchmark(service, particles, contour);

                // Export to telemetry/variation_m_N{N}.csv
                CsvExporter.exportVariationMTelemetry(n, m, avgTimeMs);

                System.out.printf("[Variation M] N: %d | M: %2d/%d | Avg Time: %.4f ms%n", n, m, maxM, avgTimeMs);
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
            CellIndexService2<SizedParticle> service = new CellIndexService2<>(optimalM, l, RC, particles);

            double avgTimeMs = runBenchmark(service, particles, contour);

            CsvExporter.exportVariationNFreeDensityTelemetry(n, avgTimeMs);

            System.out.printf("[Variation N - Free Density] N: %d | M: %d | Avg Time: %.4f ms%n", n, optimalM, avgTimeMs);
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
            CellIndexService2<SizedParticle> service = new CellIndexService2<>(m, l, RC, particles);

            double avgTimeMs = runBenchmark(service, particles, contour);

            CsvExporter.exportVariationNFixedDensityTelemetry(n, l, m, actualDensity, avgTimeMs);

            System.out.printf("[Variation N - Fixed Density] N: %d | L: %d | M: %d | Density: %.4f | Avg Time: %.4f ms%n",
                    n, l, m, actualDensity, avgTimeMs);
        }
    }

    /**
     * Runs JVM warm-up using System.nanoTime() for nanosecond precision benchmarking.
     * Returns execution time per iteration in floating-point milliseconds.
     */
    private double runBenchmark(CellIndexService2<SizedParticle> service, Set<SizedParticle> particles,
                                boolean contour) {

        // 1. Warmup phase (let JIT optimize without recording timing)
        for (int w = 0; w < StressTest.WARMUP_ITERATIONS; w++) {
            calculateAllNeighbors(service, particles, contour);
        }

        // Clear potential garbage from warmup before benchmarking
        System.gc();

        // Array to store elapsed time of each individual benchmark run (in milliseconds)
        double[] runTimesMs = new double[StressTest.BENCHMARK_ITERATIONS];

        // 2. Collect precise timing for each individual iteration
        for (int i = 0; i < StressTest.BENCHMARK_ITERATIONS; i++) {
            long startNano = System.nanoTime();

            calculateAllNeighbors(service, particles, contour);

            long elapsedNano = System.nanoTime() - startNano;
            runTimesMs[i] = elapsedNano / 1_000_000.0; // convert to ms
        }

        // 3. Calculate Mean Execution Time
        double sum = 0.0;
        for (double time : runTimesMs) {
            sum += time;
        }

        return sum / StressTest.BENCHMARK_ITERATIONS;
    }

    private void calculateAllNeighbors(CellIndexService2<SizedParticle> service,
                                       Set<SizedParticle> particles,
                                       boolean contour) {
        for (SizedParticle p : particles) {
            p.getNeighbors().clear();
            service.calculateNeighbors(p, contour);
        }
    }
}