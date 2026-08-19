package ar.edu.itba.sds;

import ar.edu.itba.sds.utils.ArgsParser;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService;
import ar.edu.itba.sds.utils.CsvExporter;
import ar.edu.itba.sds.utils.RandomParticleGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        // Parse arguments
        ArgsParser parser = new ArgsParser(args);
        final int m = parser.getM();
        final int l = parser.getL();
        final float rc = parser.getRc();
        final float riMin = parser.getRiMin();
        final float riMax = parser.getRiMax();
        final int n = parser.getN();
        final boolean contour = parser.hasContour();
        final Random random = new Random();

        // 1. Generate particles
        Set<SizedParticle> particles = RandomParticleGenerator.generate(n, l, riMin, riMax, random.nextInt());

        System.out.println("N particles: " + particles.size());
        System.out.println("Grid size: " + l + " x " + l);
        System.out.println("m: " + m);
        System.out.println("r: " + rc);

        // 2. Compute neighbors
        final CellIndexService<SizedParticle> service = new CellIndexService<>(m, l, rc, particles);

        Instant start = Instant.now();
        service.calculateNeighbors(contour);
        Instant end = Instant.now();

        long executionTimeMs = Duration.between(start, end).toMillis();
        System.out.println("Time taken to calculate neighbors: " + executionTimeMs + " ms");

        // 3. Export data
        CsvExporter.exportExecutionTelemetry(n, l, m, rc, riMin, riMax, executionTimeMs);
        CsvExporter.exportParticleData(particles, 0);

        // 4. Print results
        for (SizedParticle p : particles) {
            System.out.println("Particle: " + p);
            System.out.println("Neighbors: " + p.getNeighbors());
        }
    }
}