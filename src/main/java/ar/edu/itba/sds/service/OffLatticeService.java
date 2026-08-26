package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.*;

public class OffLatticeService <T extends SizedParticle> {

    public OffLatticeService() {
    }

    public Set<SizedParticle> getNewStandardListOfParticles(float deltaTime, float eta, Random random, Collection<T> particles){

        Set<SizedParticle> toReturn = new LinkedHashSet<>();
        for (SizedParticle p: particles){
            toReturn.add(p.getNewPositionStandard(deltaTime,eta,random));
        }
        return toReturn;
    }

    public Set<SizedParticle> getNewVotanteListOfParticles(float deltaTime,  float eta, Random random,Collection<T> particles){

        Set<SizedParticle> toReturn = new LinkedHashSet<>();
        for (SizedParticle p: particles){
            toReturn.add(p.getNewPositionVotante(deltaTime,eta,random));
        }
        return toReturn;
    }

    public float getPolarization(Collection<T> particles) {
        double sumX = 0;
        double sumY = 0;

        for (SizedParticle particle : particles) {
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

    public Set<Set<SizedParticle>> getClusters(Collection<T> particles) {

        Set<Set<SizedParticle>> clusters = new HashSet<>();
        Set<SizedParticle> visited = new HashSet<>();

        for (SizedParticle particle : particles) {

            if (visited.contains(particle)) {
                continue;
            }

            Set<SizedParticle> cluster = new HashSet<>();
            Queue<SizedParticle> queue = new LinkedList<>();

            queue.add(particle);
            visited.add(particle);

            while (!queue.isEmpty()) {

                SizedParticle current = queue.poll();
                cluster.add(current);

                for (SizedParticle neighbor : current.getNeighbors()) {

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
