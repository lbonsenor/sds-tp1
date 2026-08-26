package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.service.OffLatticeService;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@CommandLine.Command(
        name = "sds-simulation",
        mixinStandardHelpOptions = true, // Adds -h, --help, -V, --version automatically
        version = "1.0.0",
        description = "Off-lattice particle simulation runner"
)
public class ArgsParser implements Runnable {

    @Option(names = {"-l", "--length"}, description = "Grid length", defaultValue = "20")
    private int l;

    @Option(names = {"-rc", "--cut-off"}, description = "Cut-off distance", defaultValue = "3.0")
    private float rc;

    @Option(names = {"-ri-min", "--min-radius"}, description = "Minimum radius", defaultValue = "0.23")
    private float riMin;

    @Option(names = {"-ri-max", "--max-radius"}, description = "Maximum radius", defaultValue = "0.26")
    private float riMax;

    @Option(names = {"-m"}, description = "Cell grid split factor")
    private Integer m; // Nullable so we can detect if passed explicitly

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

    @Override
    public void run() {
        // Calculate default m if user didn't specify -m
        if (m == null) {
            m = (int) Math.floor(l / (rc + 2 * riMax));
        }

        final Random random = new Random(seed);

        // 1. Generate particles
        Set<Entity2D> particles = RandomParticleGenerator.generate(n, l, riMin, riMax, random.nextInt());

        System.out.println("N particles: " + particles.size());
        System.out.println("Grid size: " + l + " x " + l);
        System.out.println("m: " + m);
        System.out.println("r: " + rc);

        // 2. Compute neighbors
        final CellIndexService<Entity2D> service = new CellIndexService<>(m, l, rc, particles);
        final OffLatticeService offLatticeService = new OffLatticeService();

        //S is the biggest cluster in the network
        Set<Entity2D> s = new HashSet<>();

        for (float t = 0; t < entireT; t += deltaT) {

            Instant start = Instant.now();
            service.calculateNeighbors(contour, particles);
            Instant end = Instant.now();

            long executionTimeMs = Duration.between(start, end).toMillis();
            System.out.println("Time taken to calculate neighbors: " + executionTimeMs + " ms");

            // 3. Export data
            CsvExporter.exportExecutionTelemetry(n, l, m, rc, riMin, riMax, executionTimeMs);
            CsvExporter.exportParticleData(particles, 0);

//             4. Print results
            for (Entity2D p : particles) {
                System.out.println("Particle: " + p);
                System.out.println("Neighbors: " + p.getNeighbors().size());
            }

            System.out.println("Polarization: " + offLatticeService.getPolarization(particles));

            // 5. Print clusters
            Set<Set<Entity2D>> clusters = offLatticeService.getClusters(particles);



            int counter = 0;
            for (Set<Entity2D> cluster : clusters) {
                //System.out.println("cluster " + counter + ": " + cluster.size());
                if (cluster.size() > s.size()) {
                    s = cluster;
                }
                counter++;
            }
            System.out.println("Biggest Cluster size: " + s.size());
            System.out.println("Biggest cluster: " + s);


            particles = offLatticeService.getNewStandardListOfParticles(deltaT, eta, random, particles);
        }
    }

    // Getters
    public int getL() { return l; }
    public float getRc() { return rc; }
    public float getRiMin() { return riMin; }
    public float getRiMax() { return riMax; }
    public int getM() {
        if (m == null) {
            return (int) Math.floor(l / (rc + 2 * riMax));
        }
        return m;
    }
    public int getN() { return n; }
    public boolean hasContour() { return contour; }
    public float getDeltaT() { return deltaT; }
    public float getEntireT() { return entireT; }
    public float getEta() { return eta; }
}