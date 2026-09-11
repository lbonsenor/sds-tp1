package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.entities.Entity2D;
import ar.edu.itba.sds.model.entities.Particle;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.model.telemetry.ExecutionTime;
import ar.edu.itba.sds.model.telemetry.RunConfig;
import ar.edu.itba.sds.service.CellIndexService;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Deprecated
public class SimulationRunner {

    private final RunConfig config;
    private final TelemetryCollector collector = new TelemetryCollector();
    private final ParticleStateExporter stateExporter = new ParticleStateExporter();

    public SimulationRunner(RunConfig config) {
        this.config = config;
    }


    public void execute() {
        if (config.getMinRadius() == 0.0f && config.getMaxRadius() == 0.0f) {
            runSimulation((x, y, r, v, angle) -> new Particle(x, y, v, angle));
        } else {
            runSimulation(SizedParticle::new);
        }
    }

    private <T extends Entity2D> void runSimulation(RandomParticleGenerator.ParticleFactory<T> factory) {
        Random random = new Random(config.getSeed());

        Set<T> particles = RandomParticleGenerator.generate(
                config.getNParticles(),
                config.getLength(),
                config.getMinRadius(),
                config.getMaxRadius(),
                random.nextInt(),
                factory
        );

        CellIndexService<T> cellService = new CellIndexService<>(
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

    private <T extends Entity2D> Set<T> step(
            float t,
            int timestep,
            Set<T> particles,
            CellIndexService<T> cellService,
            Random random
    ) {
        // 1. Calculate cell grid neighbors with timing
        Instant start = Instant.now();
        cellService.calculateNeighbors(config.isPeriodicBoundary(), particles);
        Instant end = Instant.now();

        long executionTimeNs = Duration.between(start, end).toNanos();
        double executionTimeSec = executionTimeNs / 1_000_000_000.0;

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

        // 4. Update off-lattice positions using selected Strategy & Periodic Boundary settings

    }
}