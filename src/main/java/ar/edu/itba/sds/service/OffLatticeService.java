package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.*;

public class OffLatticeService {

    public OffLatticeService() {
    }

    public Set<Entity2D> getNewStandardListOfParticles(float deltaTime, float eta, Random random, Collection<Entity2D> particles){

        Set<Entity2D> toReturn = new LinkedHashSet<>();
        for (Entity2D p: particles){
            toReturn.add(p.getNewPositionStandard(deltaTime,eta,random));
        }
        return toReturn;
    }

    public Set<Entity2D> getNewVotanteListOfParticles(float deltaTime,  float eta, Random random,Collection<Entity2D> particles){

        Set<Entity2D> toReturn = new LinkedHashSet<>();
        for (Entity2D p: particles){
            toReturn.add(p.getNewPositionVotante(deltaTime,eta,random));
        }
        return toReturn;
    }

    public float getPolarization(Collection<Entity2D> particles) {
        double sumX = 0;
        double sumY = 0;

        for (Entity2D particle : particles) {
            double angle = particle.getAngle();

            sumX += Math.cos(angle);
            sumY += Math.sin(angle);
        }

        double polarizationX = sumX / particles.size();
        double polarizationY = sumY / particles.size();

        return (float) Math.sqrt(
                polarizationX * polarizationX +
                        polarizationY * polarizationY
        );
    }






    public Set<Set<Entity2D>> getClusters(Collection<Entity2D> particles) {

        Set<Set<Entity2D>> clusters = new HashSet<>();
        Set<Entity2D> visited = new HashSet<>();

        for (Entity2D particle : particles) {

            if (visited.contains(particle)) {
                continue;
            }

            Set<Entity2D> cluster = new HashSet<>();
            Queue<Entity2D> queue = new LinkedList<>();

            queue.add(particle);
            visited.add(particle);

            while (!queue.isEmpty()) {

                Entity2D current = queue.poll();
                cluster.add(current);

                for (Entity2D neighbor : current.getNeighbors()) {

                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }
}
