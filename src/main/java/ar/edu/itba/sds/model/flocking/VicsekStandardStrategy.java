package ar.edu.itba.sds.model.flocking;

import ar.edu.itba.sds.model.entities.Entity2D;

import java.util.Random;

public class VicsekStandardStrategy implements FlockingStrategy {
    @Override
    public float computeNewAngle(Entity2D particle, float eta, Random random) {
        float sinAccum = (float) Math.sin(particle.getAngle()); //[cite: 7]
        float cosAccum = (float) Math.cos(particle.getAngle()); //[cite: 7]

        for (Entity2D p : particle.getNeighbors()) { //[cite: 7]
            sinAccum += (float) Math.sin(p.getAngle()); //[cite: 7]
            cosAccum += (float) Math.cos(p.getAngle()); //[cite: 7]
        }

        float deltaTheta = (random.nextFloat() - 0.5f) * eta; //[cite: 7]
        float rawAngle = (float) Math.atan2(sinAccum, cosAccum) + deltaTheta; //[cite: 7]

        // Wrap to [-PI, PI]
        return (float) Math.atan2(Math.sin(rawAngle), Math.cos(rawAngle));
    }
}