package ar.edu.itba.sds.model.flocking;

import ar.edu.itba.sds.model.entities.Entity2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class VoterStrategy implements FlockingStrategy {
    @Override
    public float computeNewAngle(Entity2D particle, float eta, Random random) {
        float deltaTheta = (random.nextFloat() - 0.5f) * eta; 
        Set<Entity2D> neighbors = particle.getNeighbors(); 

        float rawAngle;
        if (neighbors.isEmpty()) { 
            rawAngle = particle.getAngle() + deltaTheta; 
        } else {
            List<Entity2D> neighborList = new ArrayList<>(neighbors); 
            Entity2D chosen = neighborList.get(random.nextInt(neighborList.size())); 
            rawAngle = chosen.getAngle() + deltaTheta; 
        }

        // Wrap to [-PI, PI]
        return (float) Math.atan2(Math.sin(rawAngle), Math.cos(rawAngle));
    }
}