package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.entities.Entity2D;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.model.telemetry.ClusterDetail;
import ar.edu.itba.sds.model.telemetry.ExecutionTime;
import ar.edu.itba.sds.model.telemetry.RunConfig;
import ar.edu.itba.sds.model.telemetry.TimeObservable;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.service.OffLatticeService;
import ar.edu.itba.sds.model.flocking.FlockingStrategy;
import ar.edu.itba.sds.model.flocking.VicsekStandardStrategy;
import ar.edu.itba.sds.model.flocking.VoterStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class SimulationRunner {

    private final RunConfig config;
    private final TelemetryCollector collector = new TelemetryCollector();
    private final ParticleStateExporter stateExporter = new ParticleStateExporter();
    private final OffLatticeService offLatticeService = new OffLatticeService();
    private final FlockingStrategy strategy;

    public SimulationRunner(RunConfig config) {
        this.config = config;
        this.strategy = resolveStrategy(config);
    }

    private FlockingStrategy resolveStrategy(RunConfig config) {
        if (config.getModel() == null) {
            return new VicsekStandardStrategy();
        }
        return switch (config.getModel()) {
            case VOTER -> new VoterStrategy();
            case STANDARD -> new VicsekStandardStrategy();
        };
    }

    public void execute() {
        Random random = new Random(config.getSeed());
        Set<SizedParticle> particles = RandomParticleGenerator.generate(
                config.getNParticles(),
                config.getLength(),
                config.getMinRadius(),
                config.getMaxRadius(),
                random.nextInt()
        );

        System.out.println("N particles: " + particles.size());
        System.out.println("Grid size: " + config.getLength() + " x " + config.getLength());
        System.out.println("m: " + config.getCellGridSplit());
        System.out.println("r: " + config.getCutOff());

        CellIndexService<SizedParticle> cellService = new CellIndexService<>(
                config.getCellGridSplit(),
                config.getLength(),
                config.getCutOff(),
                particles
        );

        int timestep = 0;
        for (float t = 0; t < config.getTotalTime(); t += config.getDeltaT(), timestep++) {
            particles = step(t, timestep, particles, cellService, random);
        }

        collector.exportAll(config);
    }

    private Set<SizedParticle> step(
            float t,
            int timestep,
            Set<SizedParticle> particles,
            CellIndexService<SizedParticle> cellService,
            Random random
    ) {
        // 1. Calculate cell grid neighbors with timing
        Instant start = Instant.now();
        cellService.calculateNeighbors(config.isPeriodicBoundary(), particles);
        Instant end = Instant.now();

        long executionTimeNs = Duration.between(start, end).toNanos();
        double executionTimeSec = executionTimeNs / 1_000_000_000.0;
        System.out.println("Time taken to calculate neighbors: " + executionTimeNs + " ns");

        collector.recordExecutionTime(new ExecutionTime(
                config.getRunId(),
                config.getModel(),
                config.getDensity(),
                config.getNParticles(),
                "CIM",
                executionTimeSec,
                timestep,
                config.getLength(),
                config.getCutOff()
        ));

        // 2. Export particle points
        collector.recordParticlePoints(stateExporter.extractPoints(config.getRunId(), t, particles));

        // 3. Process observables and clusters
        processObservables(t, particles);

        // 4. Update off-lattice positions using selected Strategy & Periodic Boundary settings
        if (config.isPeriodicBoundary()) {
            return offLatticeService.step(
                    particles,
                    strategy,
                    config.getDeltaT(),
                    config.getEta(),
                    random,
                    config.getLength()
            );
        }

        return offLatticeService.step(
                particles,
                strategy,
                config.getDeltaT(),
                config.getEta(),
                random
        );
    }

    private void processObservables(float t, Set<SizedParticle> particles) {
        double va = offLatticeService.getPolarization(particles);
        Set<Set<Entity2D>> clusters = offLatticeService.getClusters(particles);

        int maxClusterSize = 0;
        int clusterId = 0;
        Set<Entity2D> biggestCluster = Set.of();
        List<ClusterDetail> details = new ArrayList<>();

        for (Set<Entity2D> cluster : clusters) {
            int size = cluster.size();
            if (size > maxClusterSize) {
                maxClusterSize = size;
                biggestCluster = cluster;
            }
            details.add(new ClusterDetail(config.getRunId(), t, clusterId++, size));
        }

        System.out.println("Biggest Cluster size: " + maxClusterSize);
        System.out.println("Biggest cluster: " + biggestCluster);

        collector.recordClusterDetails(details);

        double clusterRatio = (double) maxClusterSize / config.getNParticles();
        collector.recordObservable(new TimeObservable(
                config.getRunId(),
                config.getModel() != null ? config.getModel().name() : "STANDARD",
                config.getDensity(),
                config.getEta(),
                t,
                va,
                clusterRatio,
                maxClusterSize
        ));
    }
}