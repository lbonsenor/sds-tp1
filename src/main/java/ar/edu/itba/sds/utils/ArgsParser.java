package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.flocking.FlockingModel;
import ar.edu.itba.sds.model.telemetry.RunConfig;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@CommandLine.Command(
        name = "sds-simulation",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Off-lattice particle simulation runner"
)
public class ArgsParser implements Runnable {

    private static final DateTimeFormatter RUN_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    @Option(names = {"-l", "--length"}, description = "Grid length", defaultValue = "20")
    private int l;

    @Option(names = {"-rc", "--cut-off"}, description = "Cut-off distance", defaultValue = "3.0")
    private float rc;

    @Option(names = {"-ri-min", "--min-radius"}, description = "Minimum radius", defaultValue = "0.23")
    private float riMin;

    @Option(names = {"-ri-max", "--max-radius"}, description = "Maximum radius", defaultValue = "0.26")
    private float riMax;

    @Option(names = {"-m"}, description = "Cell grid split factor")
    private Integer m;

    @Option(names = {"-n"}, description = "Number of particles", defaultValue = "7")
    private int n;

    @Option(names = {"-c", "--contour"}, description = "Enable contour (periodic boundary conditions)")
    private boolean contour;

    @Option(names = {"--delta-t"}, description = "Delta time step", defaultValue = "0.5")
    private float deltaT;

    @Option(names = {"--entire-t"}, description = "Total execution time", defaultValue = "5.0")
    private float entireT;

    @Option(names = {"--eta"}, description = "Noise parameter eta", defaultValue = "2.0")
    private float eta;

    @Option(names = {"--seed", "-s"}, description = "Random seed for reproducibility", defaultValue = "42")
    private long seed;

    @Option(names = {"--model"}, description = "Flocking model type", defaultValue = "STANDARD")
    private FlockingModel model;

    @Option(names = {"-i", "--iterations"}, description = "Number of simulation iterations", defaultValue = "1")
    private int iterations;

    @Option(names = {"--run-prefix"}, description = "Prefix of the run_id", defaultValue = "")
    private String prefix;

    @Override
    public void run() {
        int splitFactor = getM();

        String prefixPart = "";
        if (prefix != null && !prefix.isBlank()) {
            prefixPart = prefix.endsWith("_") ? prefix : prefix + "_";
        }

        for (int i = 0; i < iterations; i++) {
            String runId = prefixPart + RUN_ID_FORMATTER.format(Instant.now()) + "_run" + i;
            long currentSeed = seed + i; // Lo hago para que no sean iguales

            RunConfig config = new RunConfig(
                    runId, model, l, rc, riMin, riMax, splitFactor, n, contour, deltaT, entireT, eta, currentSeed
            );

            new SimulationRunner(config).execute();
        }
    }

    public int getM() {
        if (m == null) {
            return (int) Math.floor(l / (rc + 2 * riMax));
        }
        return m;
    }

    // Standard getters retained...
    public int getL() { return l; }
    public float getRc() { return rc; }
    public float getRiMin() { return riMin; }
    public float getRiMax() { return riMax; }
    public int getN() { return n; }
    public boolean hasContour() { return contour; }
    public float getDeltaT() { return deltaT; }
    public float getEntireT() { return entireT; }
    public float getEta() { return eta; }
    public FlockingModel getModel() { return model; }
}