package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.Entity2D;
import ar.edu.itba.sds.model.flocking.FlockingStrategy;

import java.util.*;

public class OffLatticeService {

    public OffLatticeService() {
    }
    public <T extends Entity2D> Set<T> step(
            Collection<T> particles,
            FlockingStrategy strategy,
            float deltaTime,
            float eta,
            Random random) {

        Set<T> toReturn = new LinkedHashSet<>(particles.size());
        for (T p : particles) {
            float newAngle = strategy.computeNewAngle(p, eta, random);
            @SuppressWarnings("unchecked")
            T next = (T) p.move(deltaTime, newAngle);
            toReturn.add(next);
        }
        return toReturn;
    }

    public <T extends Entity2D> Set<T> step(
            Collection<T> particles,
            FlockingStrategy strategy,
            float deltaTime,
            float eta,
            Random random,
            float contourLength) {

        Set<T> toReturn = new LinkedHashSet<>(particles.size());
        for (T p : particles) {
            float newAngle = strategy.computeNewAngle(p, eta, random);
            @SuppressWarnings("unchecked")
            T next = (T) p.move(deltaTime, newAngle, contourLength);
            toReturn.add(next);
        }
        return toReturn;
    }

    // --- Metrics & Analysis (Unchanged) ---

    public float getPolarization(Collection<? extends Entity2D> particles) {
        if (particles.isEmpty()) return 0.0f;

        double sumX = 0;
        double sumY = 0;

        for (Entity2D particle : particles) {
            sumX += Math.cos(particle.getAngle());
            sumY += Math.sin(particle.getAngle());
        }

        double polarizationX = sumX / particles.size();
        double polarizationY = sumY / particles.size();

        return (float) Math.hypot(polarizationX, polarizationY);
    }

    public Set<Set<Entity2D>> getClusters(Collection<? extends Entity2D> particles) {
        Set<Set<Entity2D>> clusters = new HashSet<>();
        Set<Entity2D> visited = new HashSet<>();

        for (Entity2D particle : particles) {
            if (visited.contains(particle)) continue;

            Set<Entity2D> cluster = new HashSet<>();
            Queue<Entity2D> queue = new ArrayDeque<>();

            queue.add(particle);
            visited.add(particle);

            while (!queue.isEmpty()) {
                Entity2D current = queue.poll();
                cluster.add(current);

                for (Entity2D neighbor : current.getNeighbors()) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            clusters.add(cluster);
        }

        return clusters;
    }
}