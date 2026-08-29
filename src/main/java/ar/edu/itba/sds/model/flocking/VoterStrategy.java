package ar.edu.itba.sds.model.flocking;

import ar.edu.itba.sds.model.entities.Entity2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class VoterStrategy implements FlockingStrategy {
    @Override
    public float computeNewAngle(Entity2D particle, float eta, Random random) {
        float deltaTheta = (random.nextFloat() - 0.5f) * eta; //[cite: 8]
        Set<Entity2D> neighbors = particle.getNeighbors(); //[cite: 8]

        float rawAngle;
        if (neighbors.isEmpty()) { //[cite: 8]
            rawAngle = particle.getAngle() + deltaTheta; //[cite: 8]
        } else {
            List<Entity2D> neighborList = new ArrayList<>(neighbors); //[cite: 8]
            Entity2D chosen = neighborList.get(random.nextInt(neighborList.size())); //[cite: 8]
            rawAngle = chosen.getAngle() + deltaTheta; //[cite: 8]
        }

        // Wrap to [-PI, PI]
        return (float) Math.atan2(Math.sin(rawAngle), Math.cos(rawAngle));
    }
}