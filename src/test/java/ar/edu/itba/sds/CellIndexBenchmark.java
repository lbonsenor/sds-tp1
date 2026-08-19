package ar.edu.itba.sds;

import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.utils.RandomParticleGenerator;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CellIndexBenchmark {

    private static final float L_DEFAULT = 20.0f;
    private static final float RC = 1.0f;
    private static final float RI_MIN = 0.23f;
    private static final float RI_MAX = 0.26f;
    private static final boolean CONTOUR = false;
    private static final float TARGET_DENSITY = 400.0f / (L_DEFAULT * L_DEFAULT);

    // =========================================================================
    // SECTION 3: VARIATION OF M
    // =========================================================================
    @State(Scope.Thread)
    public static class MVariationState {
        @Param({"400", "1000"})
        public int n;

        @Param({"1", "2", "3", "4", "5", "6", "7", "8", "9"})
        public int m;

        public CellIndexService<SizedParticle> service;
        public Set<SizedParticle> particles;

        @Setup(Level.Trial)
        public void setup() {
            particles = generateParticles(n, L_DEFAULT);
            service = new CellIndexService<>(m, L_DEFAULT, RC, particles);
        }

        @Setup(Level.Invocation)
        public void clearNeighbors() {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }
        }
    }

    @Benchmark
    public void testVariationOfM(MVariationState state) {
        state.service.calculateNeighbors(CONTOUR);
    }

    // =========================================================================
    // SECTION 4.1: VARIATION OF N (FREE DENSITY)
    // =========================================================================
    @State(Scope.Thread)
    public static class FreeDensityState {
        @Param({"10", "50", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"})
        public int n;

        @Param({"OPTIMAL", "BRUTE_FORCE"})
        public String mode;

        public CellIndexService<SizedParticle> service;
        public Set<SizedParticle> particles;

        @Setup(Level.Trial)
        public void setup() {
            particles = generateParticles(n, L_DEFAULT);
            int m = "BRUTE_FORCE".equals(mode) ? 1 : (int) Math.floor(L_DEFAULT / (RC + 2 * RI_MAX));
            service = new CellIndexService<>(m, L_DEFAULT, RC, particles);
        }

        @Setup(Level.Invocation)
        public void clearNeighbors() {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }
        }
    }

    @Benchmark
    public void testVariationOfNFreeDensity(FreeDensityState state) {
        state.service.calculateNeighbors(CONTOUR);
    }

    // =========================================================================
    // SECTION 4.2: VARIATION OF N (FIXED DENSITY)
    // =========================================================================
    @State(Scope.Thread)
    public static class FixedDensityState {
        @Param({"50", "100", "200", "400", "600", "800", "1000", "1200", "1600"})
        public int n;

        @Param({"OPTIMAL", "BRUTE_FORCE"})
        public String mode;

        public CellIndexService<SizedParticle> service;
        public Set<SizedParticle> particles;

        @Setup(Level.Trial)
        public void setup() {
            int l = (int) Math.round(Math.sqrt(n / TARGET_DENSITY));
            particles = generateParticles(n, l);
            int m = "BRUTE_FORCE".equals(mode) ? 1 : (int) Math.floor(l / (RC + 2 * RI_MAX));
            service = new CellIndexService<>(m, l, RC, particles);
        }

        @Setup(Level.Invocation)
        public void clearNeighbors() {
            for (SizedParticle p : particles) {
                p.getNeighbors().clear();
            }
        }
    }

    @Benchmark
    public void testVariationOfNFixedDensity(FixedDensityState state) {
        state.service.calculateNeighbors(CONTOUR);
    }

    // =========================================================================
    // HELPER METHODS & MAIN RUNNER
    // =========================================================================
    private static Set<SizedParticle> generateParticles(int n, float l) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return RandomParticleGenerator.generate(n, l, RI_MIN, RI_MAX);
            } catch (IllegalStateException ignored) {
            }
        }
        throw new IllegalStateException("Failed to generate particles for N=" + n + ", L=" + l);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CellIndexBenchmark.class.getSimpleName())
                .result("benchmark_results.csv")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.CSV)
                .build();

        new Runner(opt).run();
    }
}