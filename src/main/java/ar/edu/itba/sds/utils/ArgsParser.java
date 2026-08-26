package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.model.flocking.FlockingModel;
import ar.edu.itba.sds.model.telemetry.ClusterDetail;
import ar.edu.itba.sds.model.telemetry.ExecutionTime;
import ar.edu.itba.sds.model.telemetry.ParticlePoint;
import ar.edu.itba.sds.model.telemetry.RunConfig;
import ar.edu.itba.sds.model.telemetry.TimeObservable;
import ar.edu.itba.sds.model.telemetry.TrajectoryPoint;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.service.OffLatticeService;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "sds-simulation",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Off-lattice particle simulation runner"
)
public class ArgsParser implements Runnable {

    private static final DateTimeFormatter RUN_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneOffset.UTC);

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

    @Override
    public void run() {
        if (m == null) {
            m = (int) Math.floor(l / (rc + 2 * riMax));
        }

        final String runId = RUN_ID_FORMATTER.format(Instant.now());
        final Random random = new Random(seed);

        // 1. Export Run Metadata Config
        RunConfig config = new RunConfig(
                runId, model, l, rc, riMin, riMax, m, n, contour, deltaT, entireT, eta, seed
        );
        CsvExporter.exportTelemetry(List.of(config), "run_config.csv");

        // 2. Generate particles
        Set<SizedParticle> particles = RandomParticleGenerator.generate(n, l, riMin, riMax, random.nextInt());

        System.out.println("N particles: " + particles.size());
        System.out.println("Grid size: " + l + " x " + l);
        System.out.println("m: " + m);
        System.out.println("r: " + rc);

        final CellIndexService<SizedParticle> service = new CellIndexService<>(m, l, rc, particles);
        final OffLatticeService offLatticeService = new OffLatticeService();

        // Telemetry collectors
        List<ExecutionTime> executionTimes = new ArrayList<>();
        List<TimeObservable> timeObservables = new ArrayList<>();
        List<ClusterDetail> clusterDetails = new ArrayList<>();
        List<TrajectoryPoint> trajectoryPoints = new ArrayList<>();
        List<ParticlePoint> particlePoints = new ArrayList<>();

        int timestep = 0;
        for (float t = 0; t < entireT; t += deltaT, timestep++) {

            Instant start = Instant.now();
            service.calculateNeighbors(contour, particles);
            Instant end = Instant.now();

            long executionTimeNs = Duration.between(start, end).toNanos();
            double executionTimeSec = executionTimeNs / 1_000_000_000.0;
            System.out.println("Time taken to calculate neighbors: " + executionTimeNs + " ns");

            executionTimes.add(new ExecutionTime(
                    runId, model, config.getDensity(), n, "CIM", executionTimeSec, timestep, l, rc
            ));

            List<SizedParticle> particleList = new ArrayList<>(particles);

            // Fast O(1) index map to replace indexOf lookups
            Map<Entity2D, Integer> particleIndices = new HashMap<>();
            for (int i = 0; i < particleList.size(); i++) {
                particleIndices.put(particleList.get(i), i);
            }

            // Collect Particle Data & Trajectories
            for (int i = 0; i < particleList.size(); i++) {
                SizedParticle p = particleList.get(i);
                double x = (p.getMaxX() + p.getMinX()) / 2.0;
                double y = (p.getMaxY() + p.getMinY()) / 2.0;
                double radius = (p.getMaxX() - p.getMinX()) / 2.0;

                String neighborsStr = p.getNeighbors().stream()
                        .map(particleIndices::get)
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .collect(Collectors.joining(" "));

                if (neighborsStr.isEmpty()) {
                    neighborsStr = "none";
                } else {
                    neighborsStr = "[" + neighborsStr + "]";
                }

                particlePoints.add(new ParticlePoint(runId, t, i, x, y, radius, neighborsStr));
                trajectoryPoints.add(new TrajectoryPoint(runId, t, i, x, y, 0.0, 0.0, 0.0));
            }

            // Calculate Observables & Clusters
            double va = offLatticeService.getPolarization(particles);
            Set<Set<Entity2D>> clusters = offLatticeService.getClusters(particles);

            int maxClusterSize = 0;
            int clusterId = 0;
            Set<Entity2D> biggestCluster = Set.of();

            for (Set<Entity2D> cluster : clusters) {
                int clusterSize = cluster.size();
                if (clusterSize > maxClusterSize) {
                    maxClusterSize = clusterSize;
                    biggestCluster = cluster;
                }
                clusterDetails.add(new ClusterDetail(runId, t, clusterId++, clusterSize));
            }

            System.out.println("Biggest Cluster size: " + maxClusterSize);
            System.out.println("Biggest cluster: " + biggestCluster);

            double clusterRatio = (double) maxClusterSize / n;
            timeObservables.add(new TimeObservable(
                    runId, model != null ? model.name() : "STANDARD", config.getDensity(), eta, t, va, clusterRatio, maxClusterSize
            ));

            // Advance system
            particles = offLatticeService.getNewStandardListOfParticles(deltaT, eta, random, particles);
        }

        // 3. Batch export telemetry
        CsvExporter.exportTelemetry(executionTimes, "execution_times_cim.csv");
        CsvExporter.exportTelemetry(timeObservables, "time_observables.csv");
        CsvExporter.exportTelemetry(clusterDetails, "cluster_details.csv");
        CsvExporter.exportTelemetry(trajectoryPoints, "trajectories.csv");
        CsvExporter.exportTelemetry(particlePoints, "particle_data.csv");
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
    public FlockingModel getModel() { return model; }
}