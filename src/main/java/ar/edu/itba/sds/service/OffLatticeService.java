package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.*;

public class OffLatticeService <T extends SizedParticle> {

    public OffLatticeService() {
    }

    public Set<SizedParticle> getNewStandardListOfParticles(float deltaTime, float eta, int seed, Collection<T> particles){

        Set<SizedParticle> toReturn = new LinkedHashSet<>();
        for (SizedParticle p: particles){
            toReturn.add(p.getNewPositionStandard(deltaTime,eta,seed));
        }
        return toReturn;
    }

    public Set<SizedParticle> getNewVotanteListOfParticles(float deltaTime,  float eta, int seed,Collection<T> particles){

        Set<SizedParticle> toReturn = new LinkedHashSet<>();
        for (SizedParticle p: particles){
            toReturn.add(p.getNewPositionVotante(deltaTime,eta,seed));
        }
        return toReturn;
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
